# Use PowerShell to start a process to prevent a terminal window from opening and staying open
# for the life of the process
$psi = New-Object System.Diagnostics.ProcessStartInfo
$newproc = New-Object System.Diagnostics.Process
$length = $args.Length-1
$psi.FileName = $args[0]
$psi.Arguments = $args[1..$length]
$psi.CreateNoWindow = $true
$psi.WindowStyle = 'Hidden'
$newproc.StartInfo = $psi
$newproc.Start()
$newproc