# 等待后端 HTTP 接口就绪，避免前端先启动导致页面请求失败
param(
    [string]$Port = "9999",
    [int]$TimeoutSeconds = 120
)
$url = "http://localhost:$Port/api/projects"
$max = $TimeoutSeconds
$i = 0
while ($i -lt $max) {
    try {
        $resp = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3
        if ($resp.StatusCode -eq 200) {
            Write-Host "Backend ready after ${i}s ($url -> 200)"
            exit 0
        }
    } catch {
        # 还未就绪或连接被拒绝，继续等待
    }
    Start-Sleep -Seconds 1
    $i++
}
Write-Host "Backend did not become ready within ${max}s ($url)"
exit 1
