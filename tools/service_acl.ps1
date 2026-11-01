param([Parameter(Mandatory=$true)][string]$Target, [switch]$Tree)
$ErrorActionPreference = 'Stop'
if ($Tree) {
    $sid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    $allowed = @($sid, 'S-1-5-18', 'S-1-5-32-544', 'S-1-5-80-956008885-3418522649-1831038044-1853292631-2271478464')
    $count = 0
    $safe = $true
    $items = @((Get-Item -LiteralPath $Target -Force)) + @(Get-ChildItem -LiteralPath $Target -Recurse -Force -ErrorAction Stop)
    if ($items.Count -gt 30000) { throw 'Deployment tree limit' }
    foreach ($item in $items) {
        if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) { $safe = $false; break }
        $entry = Get-Acl -LiteralPath $item.FullName
        if ($entry.GetOwner([System.Security.Principal.SecurityIdentifier]).Value -notin $allowed) { $safe = $false; break }
        foreach ($rule in $entry.GetAccessRules($true, $true, [System.Security.Principal.SecurityIdentifier])) {
            if (($rule.PropagationFlags -band [System.Security.AccessControl.PropagationFlags]::InheritOnly) -ne 0) { continue }
            if ($rule.AccessControlType -ne 'Allow') { $safe = $false; break }
            $identity = $rule.IdentityReference.Value
            if ($identity -notin ($allowed + @('S-1-3-4')) -and (([int64]$rule.FileSystemRights -band 0x500d0156) -ne 0)) { $safe = $false; break }
        }
        $count++
        if (-not $safe) { break }
    }
    @{safe=$safe; scanned=$count} | ConvertTo-Json -Compress
    exit 0
}
$acl = Get-Acl -LiteralPath $Target
$rules = @($acl.GetAccessRules($true, $true, [System.Security.Principal.SecurityIdentifier]) | ForEach-Object {
    @{sid=$_.IdentityReference.Value; allow=($_.AccessControlType -eq 'Allow'); rights=[int64]$_.FileSystemRights; inheritOnly=(($_.PropagationFlags -band [System.Security.AccessControl.PropagationFlags]::InheritOnly) -ne 0)}
})
@{sid=[System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value; owner=$acl.GetOwner([System.Security.Principal.SecurityIdentifier]).Value; protected=$acl.AreAccessRulesProtected; rules=$rules} | ConvertTo-Json -Depth 4 -Compress
