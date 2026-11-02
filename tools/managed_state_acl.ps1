param([Parameter(Mandatory=$true)][string]$Target)
$ErrorActionPreference = 'Stop'
$sid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
$allowed = @($sid, 'S-1-5-18', 'S-1-5-32-544', 'S-1-3-4')
$root = Get-Item -LiteralPath $Target -Force
if (-not $root.PSIsContainer -or -not (Get-Acl -LiteralPath $Target).AreAccessRulesProtected) { throw 'Protected state root required' }
# An explicitly protected parent permits inherited private leaf ACLs. Inspect
# effective leaf rules too; never convert a broad ACL into a private one.
$items = @($root) + @(Get-ChildItem -LiteralPath $Target -Force -ErrorAction Stop)
if ($items.Count -gt 200002) { throw 'State audit capacity' }
$safe = $true
foreach ($item in $items) {
    if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) { $safe = $false; break }
    $acl = Get-Acl -LiteralPath $item.FullName
    if ($acl.GetOwner([System.Security.Principal.SecurityIdentifier]).Value -notin $allowed) { $safe = $false; break }
    foreach ($rule in $acl.GetAccessRules($true, $true, [System.Security.Principal.SecurityIdentifier])) {
        if ($rule.AccessControlType -ne 'Allow' -or $rule.IdentityReference.Value -notin $allowed) { $safe = $false; break }
    }
    if (-not $safe) { break }
}
@{safe=$safe; scanned=$items.Count} | ConvertTo-Json -Compress
