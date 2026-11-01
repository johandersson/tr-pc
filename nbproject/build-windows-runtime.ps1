param(
    [Parameter(Mandatory = $true)]
    [string] $JdkDir,
    [Parameter(Mandatory = $true)]
    [string] $ModulePath,
    [Parameter(Mandatory = $true)]
    [string] $OutputDir,
    [Parameter(Mandatory = $true)]
    [string] $Modules
)

$ErrorActionPreference = "Stop"

function Convert-ToWindowsPath([string] $Path) {
    if ($Path.StartsWith("/")) {
        return (wsl.exe wslpath -w $Path).Trim()
    }
    return [System.IO.Path]::GetFullPath($Path)
}

$jdk = Convert-ToWindowsPath $JdkDir
$modulePath = Convert-ToWindowsPath $ModulePath
$output = Convert-ToWindowsPath $OutputDir

$jlinkArgs = @(
    "--module-path", $modulePath,
    "--add-modules", $Modules,
    "--output", $output,
    "--strip-debug",
    "--no-header-files",
    "--no-man-pages",
    "--compress", "zip-6",
    "--include-locales", "en,de,es,fr,nl"
)

& (Join-Path $jdk "bin\jlink.exe") $jlinkArgs

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
