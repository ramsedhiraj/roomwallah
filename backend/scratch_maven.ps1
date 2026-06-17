$url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip"
$bytes = [System.Text.Encoding]::UTF8.GetBytes($url)
$hashBytes = [System.Security.Cryptography.SHA256]::Create().ComputeHash($bytes)
$hash = ($hashBytes | ForEach-Object { $_.ToString("x2") }) -join ""

$parentDir = "C:/Users/hp/.m2/wrapper/dists/apache-maven-3.9.16"
$targetDir = "$parentDir/$hash"

Write-Output "URL: $url"
Write-Output "Hash: $hash"
Write-Output "Target Directory: $targetDir"

if (-not (Test-Path -Path $parentDir)) {
    New-Item -Path $parentDir -ItemType Directory -Force | Out-Null
}

if (-not (Test-Path -Path $targetDir)) {
    New-Item -Path $targetDir -ItemType Directory -Force | Out-Null
}

# Download zip to temp file in workspace (avoiding AppData/Temp permission issues)
$zipPath = "C:/Users/hp/.gemini/antigravity/scratch/roomwallah/backend/maven.zip"
Write-Output "Downloading to $zipPath..."
Invoke-WebRequest -Uri $url -OutFile $zipPath

Write-Output "Extracting archive..."
$extractTemp = "C:/Users/hp/.gemini/antigravity/scratch/roomwallah/backend/maven_extract_temp"
if (Test-Path -Path $extractTemp) {
    Remove-Item $extractTemp -Recurse -Force | Out-Null
}
New-Item -Path $extractTemp -ItemType Directory -Force | Out-Null

Expand-Archive -Path $zipPath -DestinationPath $extractTemp

Write-Output "Moving to target directory..."
# The zip contains a folder called 'apache-maven-3.9.16'
$extractedFolder = "$extractTemp/apache-maven-3.9.16"
# Move contents of $extractedFolder to $targetDir
Get-ChildItem -Path $extractedFolder | ForEach-Object {
    Move-Item -Path $_.FullName -Destination $targetDir -Force
}

Write-Output "Cleaning up temporary files..."
Remove-Item $zipPath -Force
Remove-Item $extractTemp -Recurse -Force

Write-Output "Maven setup complete!"
