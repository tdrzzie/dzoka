param([Parameter(Mandatory)][ValidateSet('install','inspect','start','disable','terminate','remove')][string]$Operation,[Parameter(Mandatory)][string]$Request)
$ErrorActionPreference='Stop'
$r=Get-Content -LiteralPath $Request -Raw | ConvertFrom-Json
if($r.name -notmatch '^Dzoka-Interactive-[0-9a-f]{32}$' -or $r.source -notmatch '^urn:dzoka:interactive:[0-9a-f]{64}$'){throw 'Invalid task identity'}
$sid=[System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
if($r.sid -ne $sid){throw 'Wrong task account'}
$scheduler=New-Object -ComObject Schedule.Service;$scheduler.Connect();$folder=$scheduler.GetFolder('\')
function Resolve-Sid($value){
 if($value -match '^S-1-'){return $value}
 return (New-Object System.Security.Principal.NTAccount($value)).Translate([System.Security.Principal.SecurityIdentifier]).Value
}
function Check-Task($task){
 $d=$task.Definition
 if($task.Name -ne $r.name -or $task.Path -ne ('\'+$r.name) -or $d.RegistrationInfo.Source -ne $r.source -or $d.RegistrationInfo.URI -ne ('\'+$r.name)){throw 'Task ownership differs'}
 if((Resolve-Sid $d.Principal.UserId) -ne $sid -or $d.Principal.LogonType -ne 3 -or $d.Principal.RunLevel -ne 0){throw 'Task identity differs'}
 if($d.Actions.Count -ne 1 -or $d.Actions.Item(1).Type -ne 0){throw 'Task action differs'}
 $a=$d.Actions.Item(1)
 if($a.Path -ne $r.executable -or $a.Arguments -ne $r.arguments -or $a.WorkingDirectory -ne $r.workingDirectory){throw 'Task command differs'}
 if($d.Triggers.Count -ne 2 -or $d.Triggers.Item(1).Type -ne 9 -or (Resolve-Sid $d.Triggers.Item(1).UserId) -ne $sid -or !$d.Triggers.Item(1).Enabled){throw 'Task trigger differs'}
 $timer=$d.Triggers.Item(2)
 if($timer.Type -ne 1 -or !$timer.Enabled -or [DateTimeOffset]::Parse($timer.StartBoundary) -ne [DateTimeOffset]::Parse($r.watchdogStart) -or [System.Xml.XmlConvert]::ToTimeSpan($timer.Repetition.Interval).TotalSeconds -ne 60 -or $timer.Repetition.Duration -or $timer.Repetition.StopAtDurationEnd){throw 'Task watchdog differs'}
 $s=$d.Settings
 if($s.MultipleInstances -ne 2 -or !$s.AllowDemandStart -or !$s.StartWhenAvailable -or $s.DisallowStartIfOnBatteries -or $s.StopIfGoingOnBatteries -or [System.Xml.XmlConvert]::ToTimeSpan($s.ExecutionTimeLimit).TotalSeconds -ne 0 -or $s.RestartCount -ne 3 -or [System.Xml.XmlConvert]::ToTimeSpan($s.RestartInterval).TotalSeconds -ne 60){throw 'Task policy differs'}
 $acl=New-Object System.Security.AccessControl.RawSecurityDescriptor($task.GetSecurityDescriptor(5))
 if($acl.Owner.Value -notin @($sid,'S-1-5-18','S-1-5-32-544') -or !$acl.DiscretionaryAcl){throw 'Task owner/ACL differs'}
 foreach($ace in $acl.DiscretionaryAcl){if($ace.AceType.ToString() -ne 'AccessAllowed' -or $ace.SecurityIdentifier.Value -notin @($sid,'S-1-5-18','S-1-5-32-544')){throw 'Broad or complex task ACL'}}
}
if($Operation -eq 'install'){
 $d=$scheduler.NewTask(0);$d.RegistrationInfo.Source=$r.source;$d.RegistrationInfo.Description='Dzoka explicitly configured interactive managed worker. Requires login.'
 $d.Principal.UserId=$sid;$d.Principal.LogonType=3;$d.Principal.RunLevel=0
 $s=$d.Settings;$s.Enabled=$false;$s.MultipleInstances=2;$s.AllowDemandStart=$true;$s.StartWhenAvailable=$true;$s.DisallowStartIfOnBatteries=$false;$s.StopIfGoingOnBatteries=$false;$s.ExecutionTimeLimit='PT0S';$s.RestartCount=3;$s.RestartInterval='PT1M'
 $trigger=$d.Triggers.Create(9);$trigger.UserId=$sid;$trigger.Enabled=$true
 $timer=$d.Triggers.Create(1);$timer.StartBoundary=$r.watchdogStart;$timer.Enabled=$true;$timer.Repetition.Interval='PT1M';$timer.Repetition.StopAtDurationEnd=$false
 $action=$d.Actions.Create(0);$action.Path=$r.executable;$action.Arguments=$r.arguments;$action.WorkingDirectory=$r.workingDirectory
 # CREATE only: never update a task, even if its name races with registration.
 $task=$folder.RegisterTaskDefinition($r.name,$d,2,$sid,$null,3,('O:'+$sid+'G:'+$sid+'D:P(A;;FA;;;SY)(A;;FA;;;BA)(A;;FA;;;'+$sid+')'))
}else{$task=$folder.GetTask($r.name)}
Check-Task $task
if($Operation -eq 'start'){$task.Enabled=$true;$null=$task.Run($null)}
if($Operation -eq 'disable'){$task.Enabled=$false}
if($Operation -eq 'terminate'){$task.Stop(0)}
if($Operation -eq 'remove'){
 if($task.State -eq 4 -or $task.State -eq 2){throw 'Stop task before removal'}
 $folder.DeleteTask($r.name,0)
 @{schemaVersion=1;removed=$true} | ConvertTo-Json -Compress
}else{@{schemaVersion=1;state=[int]$task.State;enabled=[bool]$task.Enabled;lastResult=[long]$task.LastTaskResult;interactive=$true;leastPrivilege=$true;ownedDefinition=$true} | ConvertTo-Json -Compress}
