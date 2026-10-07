# =============================================================================
#  Express Station System - API smoke test
#  Usage: start the backend first, then run this script in PowerShell:
#      powershell -ExecutionPolicy Bypass -File scripts\api-smoke-test.ps1
#  It exercises 60+ cases across auth / RBAC / parcel / pickup / ship /
#  exception / stats / export / system management and writes a report to
#  scripts\api-smoke-result.txt
# =============================================================================
$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080/api'
$bodies = Join-Path $PSScriptRoot 'smoke-bodies'
$out = Join-Path $PSScriptRoot 'api-smoke-result.txt'
$lines = New-Object System.Collections.Generic.List[string]

function Log($m) { $lines.Add([string]$m); Write-Host $m }

# Post a request built from a UTF-8 template file with placeholder substitution,
# so that Chinese payload values survive regardless of the console code page.
function Send-Template($method, $url, $templateName, $token, $replacements) {
    $path = Join-Path $bodies $templateName
    $text = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
    if ($replacements) {
        foreach ($k in $replacements.Keys) { $text = $text.Replace($k, [string]$replacements[$k]) }
    }
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($text)
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    return Invoke-RestMethod -Uri $url -Method $method -Body $bytes -ContentType 'application/json; charset=utf-8' -Headers $headers -TimeoutSec 30
}

function Post-Template($url, $templateName, $token, $replacements) {
    return Send-Template 'POST' $url $templateName $token $replacements
}

function Put-Template($url, $templateName, $token, $replacements) {
    return Send-Template 'PUT' $url $templateName $token $replacements
}

function Post-Json($url, $obj, $token) {
    $json = $obj | ConvertTo-Json -Depth 6 -Compress
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    return Invoke-RestMethod -Uri $url -Method Post -Body $bytes -ContentType 'application/json; charset=utf-8' -Headers $headers -TimeoutSec 30
}

function Get-Api($url, $token) {
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    return Invoke-RestMethod -Uri $url -Method Get -Headers $headers -TimeoutSec 30
}

function Show($label, $obj) {
    $json = $obj | ConvertTo-Json -Depth 4 -Compress
    if ($json.Length -gt 900) { $json = $json.Substring(0, 900) + ' ...' }
    Log "$label => $json"
}

function ShowErr($label, $err) {
    $msg = $null
    if ($err.ErrorDetails -and $err.ErrorDetails.Message) { $msg = $err.ErrorDetails.Message } else { $msg = $err.Exception.Message }
    Log "$label => $msg"
}

function Check-Code($url, $token, $label, $expect) {
    try {
        $r = Get-Api $url $token
        $mark = if ($r.code -eq $expect) { 'OK' } else { 'MISMATCH' }
        Log "$label => [$mark] code=$($r.code) expect=$expect message=$($r.message)"
    } catch {
        ShowErr "$label => [HTTP ERROR] expect code=$expect" $_
    }
}

Log "==================== 1. AUTH ===================="
$login = Post-Json "$base/auth/login" @{ username = 'admin'; password = '123456' } $null
Show "1.1 admin login" @{ code = $login.code; tokenType = $login.data.tokenType; expiresIn = $login.data.expiresIn; realName = $login.data.userInfo.realName; roles = $login.data.userInfo.roles; permCount = $login.data.userInfo.permissions.Count }
$adminToken = $login.data.token

$bad = Post-Json "$base/auth/login" @{ username = 'admin'; password = 'wrongpwd' } $null
Show "1.2 wrong password" @{ code = $bad.code; message = $bad.message }

$me = Get-Api "$base/auth/me" $adminToken
Show "1.3 current user" @{ code = $me.code; username = $me.data.username; realName = $me.data.realName }

$menus = Get-Api "$base/auth/menus" $adminToken
Show "1.4 admin menus" @{ code = $menus.code; rootCount = $menus.data.Count; roots = ($menus.data | ForEach-Object { $_.permName + '(' + $_.children.Count + ')' }) -join ' | ' }

Check-Code "$base/parcels/page" $null "1.5 no-token access" 401

Log ""
Log "==================== 2. PARCEL IN-STORE ===================="
$wb = 'SF' + (Get-Random -Minimum 100000000000 -Maximum 999999999999)
$inStore = Post-Template "$base/parcels/in-store" 'body-instore.json' $adminToken @{ '__WB__' = $wb }
Show "2.1 in-store ($wb)" @{ code = $inStore.code; message = $inStore.message; id = $inStore.data.id; pickupCode = $inStore.data.pickupCode; shelfCode = $inStore.data.shelfCode; status = $inStore.data.status; statusName = $inStore.data.statusName; stationName = $inStore.data.stationName; operatorName = $inStore.data.operatorName; parcelTypeName = $inStore.data.parcelTypeName }
$parcelId = $inStore.data.id
$pickupCode = $inStore.data.pickupCode

$dup = Post-Template "$base/parcels/in-store" 'body-instore.json' $adminToken @{ '__WB__' = $wb }
Show "2.2 duplicate waybill" @{ code = $dup.code; message = $dup.message }

$badNo = Post-Template "$base/parcels/in-store" 'body-instore-badformat.json' $adminToken $null
Show "2.3 invalid waybill format" @{ code = $badNo.code; message = $badNo.message }

$badPhone = Post-Template "$base/parcels/in-store" 'body-instore-badphone.json' $adminToken @{ '__WB__' = ('SF' + (Get-Random -Minimum 100000000000 -Maximum 999999999999)) }
Show "2.4 invalid phone" @{ code = $badPhone.code; message = $badPhone.message }

Log ""
Log "==================== 3. SHELF CONSISTENCY ===================="
$shelfBefore = Get-Api "$base/shelves/list?stationId=1" $adminToken
$target = $shelfBefore.data | Where-Object { $_.shelfCode -eq $inStore.data.shelfCode }
Show "3.1 shelf after in-store" @{ shelfCode = $target.shelfCode; usedCount = $target.usedCount; capacity = $target.capacity; freeCount = $target.freeCount }

Log ""
Log "==================== 4. PICKUP ===================="
$q = Get-Api "$base/parcels/query?keyword=$pickupCode&stationId=1" $adminToken
Show "4.1 query by pickup code" @{ code = $q.code; count = $q.data.Count; waybillNo = $q.data[0].waybillNo; statusName = $q.data[0].statusName }

$fee = Get-Api "$base/parcels/$parcelId/overdue-fee" $adminToken
Show "4.2 overdue fee" @{ storageDays = $fee.data.storageDays; freeDays = $fee.data.freeDays; overdueDays = $fee.data.overdueDays; overdueFee = $fee.data.overdueFee }

$wrong = Post-Json "$base/parcels/pickup" @{ waybillNo = $wb; pickupCode = '00000000'; receiverName = 'SmokeTest' } $adminToken
Show "4.3 wrong pickup code" @{ code = $wrong.code; message = $wrong.message }

$pickup = Post-Template "$base/parcels/pickup" 'body-pickup.json' $adminToken @{ '__WB__' = $wb; '__CODE__' = $pickupCode }
Show "4.4 pickup success" @{ code = $pickup.code; message = $pickup.message; recordId = $pickup.data.id; operatorName = $pickup.data.operatorName; pickupType = $pickup.data.pickupType; verifyType = $pickup.data.verifyType }

$again = Post-Template "$base/parcels/pickup" 'body-pickup.json' $adminToken @{ '__WB__' = $wb; '__CODE__' = $pickupCode }
Show "4.5 duplicate pickup" @{ code = $again.code; message = $again.message }

$shelfAfter = Get-Api "$base/shelves/list?stationId=1" $adminToken
$target2 = $shelfAfter.data | Where-Object { $_.shelfCode -eq $inStore.data.shelfCode }
Show "4.6 shelf after pickup (must equal 3.1)" @{ shelfCode = $target2.shelfCode; usedCount = $target2.usedCount; freeCount = $target2.freeCount }

$traces = Get-Api "$base/parcels/$parcelId/traces" $adminToken
Show "4.7 traces" @{ count = $traces.data.Count; types = ($traces.data | ForEach-Object { $_.operateType }) -join ' -> '; descs = ($traces.data | ForEach-Object { $_.operateDesc }) -join ' // ' }

Log ""
Log "==================== 5. RBAC ===================="
$staff = Post-Json "$base/auth/login" @{ username = 'staff01'; password = '123456' } $null
$staffToken = $staff.data.token
Show "5.1 staff01 login" @{ code = $staff.code; roles = $staff.data.userInfo.roles; permCount = $staff.data.userInfo.permissions.Count; stationId = $staff.data.userInfo.stationId; stationName = $staff.data.userInfo.stationName }
$staffMenus = Get-Api "$base/auth/menus" $staffToken
Show "5.2 staff01 menus" @{ rootCount = $staffMenus.data.Count; roots = ($staffMenus.data | ForEach-Object { $_.permName }) -join ' | ' }

Check-Code "$base/users/page" $staffToken "5.3 staff01 -> /users/page" 403
Check-Code "$base/stats/station-rank" $staffToken "5.4 staff01 -> /stats/station-rank" 403

$user = Post-Json "$base/auth/login" @{ username = 'user01'; password = '123456' } $null
$userToken = $user.data.token
Show "5.5 user01 login" @{ code = $user.code; roles = ($user.data.userInfo.roles -join ','); perms = ($user.data.userInfo.permissions -join ',') }
$userParcels = Get-Api "$base/parcels/page?pageNum=1&pageSize=100" $userToken
$phones = ($userParcels.data.list | ForEach-Object { $_.receiverPhone } | Sort-Object -Unique) -join ','
Show "5.6 user01 parcels (phone-scoped)" @{ total = $userParcels.data.total; distinctPhones = $phones }
$userMenu = Get-Api "$base/auth/menus" $userToken
Show "5.7 user01 menus" @{ rootCount = $userMenu.data.Count; roots = ($userMenu.data | ForEach-Object { $_.permName }) -join ' | ' }

Log ""
Log "==================== 6. STATS ===================="
$ov = Get-Api "$base/stats/overview" $adminToken
Show "6.1 overview" $ov.data
$trend = Get-Api "$base/stats/trend?days=7" $adminToken
Show "6.2 trend" @{ dates = $trend.data.dates -join ','; inCounts = $trend.data.inCounts -join ','; pickupCounts = $trend.data.pickupCounts -join ',' }
$company = Get-Api "$base/stats/company" $adminToken
Show "6.3 company" @{ count = $company.data.Count; top = $company.data[0].name + '=' + $company.data[0].value }
$ptype = Get-Api "$base/stats/parcel-type" $adminToken
Show "6.4 parcel type" @{ items = ($ptype.data | ForEach-Object { $_.name + '=' + $_.value }) -join ',' }
$rank = Get-Api "$base/stats/station-rank" $adminToken
Show "6.5 station rank" @{ items = ($rank.data | ForEach-Object { $_.stationName + '(' + $_.parcelCount + '/' + $_.pickupCount + ')' }) -join ',' }

Log ""
Log "==================== 7. SHIP & EXCEPTION ===================="
$ship = Post-Template "$base/ship-orders" 'body-ship.json' $adminToken $null
Show "7.1 ship create" @{ code = $ship.code; id = $ship.data }
$shipPage = Get-Api "$base/ship-orders/page?pageNum=1&pageSize=5" $adminToken
Show "7.2 ship page" @{ total = $shipPage.data.total; statusName = $shipPage.data.list[0].statusName; operatorName = $shipPage.data.list[0].operatorName; stationName = $shipPage.data.list[0].stationName; parcelTypeName = $shipPage.data.list[0].parcelTypeName }
$shipStatus = Invoke-RestMethod -Uri "$base/ship-orders/$($ship.data)/status?status=ACCEPTED&waybillNo=SF9998887776665" -Method Put -Headers @{ Authorization = "Bearer $adminToken" } -TimeoutSec 30
Show "7.3 ship status update" @{ code = $shipStatus.code; message = $shipStatus.message }

$wb2 = 'ZT' + (Get-Random -Minimum 100000000000 -Maximum 999999999999)
$p2 = Post-Template "$base/parcels/in-store" 'body-instore-zt.json' $adminToken @{ '__WB__' = $wb2 }
Show "7.4 in-store for exception ($wb2)" @{ code = $p2.code; id = $p2.data.id }
$exc = Post-Template "$base/exceptions" 'body-exception.json' $adminToken @{ '__PID__' = $p2.data.id }
Show "7.5 exception create" @{ code = $exc.code; message = $exc.message; id = $exc.data }
$p2detail = Get-Api "$base/parcels/$($p2.data.id)" $adminToken
Show "7.6 parcel status after exception" @{ status = $p2detail.data.status; statusName = $p2detail.data.statusName }
$handle = Put-Template "$base/exceptions/$($exc.data)/handle" 'body-exception-handle.json' $adminToken $null
Show "7.7 exception handle" @{ code = $handle.code; message = $handle.message }
$p2after = Get-Api "$base/parcels/$($p2.data.id)" $adminToken
Show "7.8 parcel status after handle" @{ status = $p2after.data.status; statusName = $p2after.data.statusName }
$excPage = Get-Api "$base/exceptions/page?pageNum=1&pageSize=5" $adminToken
Show "7.9 exception page" @{ total = $excPage.data.total; typeName = $excPage.data.list[0].exceptionTypeName; statusName = $excPage.data.list[0].handleStatusName; stationName = $excPage.data.list[0].stationName }

Log ""
Log "==================== 8. EXPORT ===================="
$exportPath = Join-Path $PSScriptRoot 'api-smoke-export.xlsx'
$resp = Invoke-WebRequest -Uri "$base/parcels/export?stationId=1" -Headers @{ Authorization = "Bearer $adminToken" } -OutFile $exportPath -PassThru -UseBasicParsing -TimeoutSec 60
Show "8.1 export" @{ statusCode = $resp.StatusCode; bytes = (Get-Item $exportPath).Length }

Log ""
Log "==================== 9. SYSTEM MGMT ===================="
$users = Get-Api "$base/users/page?pageNum=1&pageSize=10" $adminToken
Show "9.1 users page" @{ total = $users.data.total; first = $users.data.list[0].username + '/' + ($users.data.list[0].roleNames -join '|') + '/station=' + $users.data.list[0].stationName }
$roles = Get-Api "$base/roles/list" $adminToken
Show "9.2 roles" @{ count = $roles.data.Count; codes = ($roles.data | ForEach-Object { $_.roleCode }) -join ',' }
$tree = Get-Api "$base/permissions/tree?type=ALL" $adminToken
Show "9.3 permission tree" @{ rootCount = $tree.data.Count; firstRootChildren = $tree.data[0].children.Count; names = ($tree.data | ForEach-Object { $_.permName }) -join ' | ' }
$rolePerms = Get-Api "$base/roles/2/permissions" $adminToken
Show "9.4 STAFF role perm ids count" @{ count = $rolePerms.data.Count }
$stations = Get-Api "$base/stations/page?pageNum=1&pageSize=10" $adminToken
Show "9.5 stations page" @{ total = $stations.data.total; first = $stations.data.list[0].stationName + ' shelfCount=' + $stations.data.list[0].shelfCount + ' usedCount=' + $stations.data.list[0].usedCount }
$shelfPage = Get-Api "$base/shelves/page?pageNum=1&pageSize=10" $adminToken
Show "9.6 shelves page" @{ total = $shelfPage.data.total; firstShelf = $shelfPage.data.list[0].shelfCode; firstFree = $shelfPage.data.list[0].freeCount }
$shelves = Get-Api "$base/shelves/available?stationId=1" $adminToken
Show "9.7 available shelves" @{ count = $shelves.data.Count }
$recalc = Invoke-RestMethod -Uri "$base/shelves/recalculate?stationId=1" -Method Put -Headers @{ Authorization = "Bearer $adminToken" } -TimeoutSec 30
Show "9.8 recalculate shelf usage" @{ code = $recalc.code; message = $recalc.message }

Log ""
Log "==================== 10. REGISTER ===================="
$newUser = 'testuser' + (Get-Random -Minimum 1000 -Maximum 9999)
$reg = Post-Json "$base/auth/register" @{ username = $newUser; password = '123456'; confirmPassword = '123456'; realName = 'RegTest'; phone = '13800009999' } $null
Show "10.1 register ($newUser)" @{ code = $reg.code; message = $reg.message; roles = ($reg.data.roles -join ',') }
$regDup = Post-Json "$base/auth/register" @{ username = $newUser; password = '123456'; confirmPassword = '123456'; realName = 'RegTest'; phone = '13800009999' } $null
Show "10.2 duplicate register" @{ code = $regDup.code; message = $regDup.message }
$r3 = Post-Json "$base/auth/register" @{ username = ('x' + (Get-Random -Minimum 1000 -Maximum 9999)); password = '123456'; confirmPassword = '654321'; realName = 'RegTest'; phone = '13800009998' } $null
Show "10.3 password mismatch" @{ code = $r3.code; message = $r3.message }
$r4 = Post-Json "$base/auth/register" @{ username = 'ab'; password = '123456'; confirmPassword = '123456'; realName = 'RegTest'; phone = '13800009997' } $null
Show "10.4 invalid username" @{ code = $r4.code; message = $r4.message }

Log ""
Log "==================== 11. USER MGMT WRITE ===================="
$created = Post-Json "$base/users" @{ username = ('mgmt' + (Get-Random -Minimum 1000 -Maximum 9999)); password = '123456'; realName = 'MgmtTest'; phone = '13800007777'; gender = 1; stationId = 1; status = 1; roleIds = @(2) } $adminToken
Show "11.1 create user" @{ code = $created.code; message = $created.message; id = $created.data }
$newId = $created.data
$upd = Invoke-RestMethod -Uri "$base/users/$newId/status?status=0" -Method Put -Headers @{ Authorization = "Bearer $adminToken" } -TimeoutSec 30
Show "11.2 disable user" @{ code = $upd.code; message = $upd.message }
$rst = Invoke-RestMethod -Uri "$base/users/$newId/password?password=abc123456" -Method Put -Headers @{ Authorization = "Bearer $adminToken" } -TimeoutSec 30
Show "11.3 reset password" @{ code = $rst.code; message = $rst.message }
$del = Invoke-RestMethod -Uri "$base/users/$newId" -Method Delete -Headers @{ Authorization = "Bearer $adminToken" } -TimeoutSec 30
Show "11.4 delete user" @{ code = $del.code; message = $del.message }
try { $delSelf = Invoke-RestMethod -Uri "$base/users/1" -Method Delete -Headers @{ Authorization = "Bearer $adminToken" } -TimeoutSec 30; Show "11.5 delete self" @{ code = $delSelf.code; message = $delSelf.message } } catch { ShowErr "11.5 delete self (expect refuse)" $_ }

Log ""
Log "==================== 12. STATION & SHELF CRUD ===================="
$sc = Post-Json "$base/stations" @{ stationCode = ('ST' + (Get-Random -Minimum 100 -Maximum 999)); stationName = 'SmokeStation'; address = 'Smoke Addr'; contactPhone = '025-12345678'; managerName = 'SmokeMgr'; businessHours = '08:00-21:00'; capacity = 100; status = 1 } $adminToken
Show "12.1 create station" @{ code = $sc.code; id = $sc.data }
$shc = Post-Json "$base/shelves" @{ stationId = $sc.data; shelfCode = 'Z-01-01'; area = 'Z'; capacity = 30; status = 1 } $adminToken
Show "12.2 create shelf" @{ code = $shc.code; id = $shc.data }
$shd = Invoke-RestMethod -Uri "$base/shelves/$($shc.data)" -Method Delete -Headers @{ Authorization = "Bearer $adminToken" } -TimeoutSec 30
Show "12.3 delete shelf" @{ code = $shd.code; message = $shd.message }
$std = Invoke-RestMethod -Uri "$base/stations/$($sc.data)" -Method Delete -Headers @{ Authorization = "Bearer $adminToken" } -TimeoutSec 30
Show "12.4 delete station" @{ code = $std.code; message = $std.message }

[System.IO.File]::WriteAllLines($out, $lines, (New-Object System.Text.UTF8Encoding($false)))
Write-Host ""
Write-Host "DONE -> $out"
