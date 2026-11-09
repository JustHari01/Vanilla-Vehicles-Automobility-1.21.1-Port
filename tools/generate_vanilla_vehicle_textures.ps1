param(
    [string]$OutputRoot = (Join-Path $PSScriptRoot '..\common\src\main\resources\assets\automobility\textures\entity\automobile\frame')
)

Add-Type -AssemblyName System.Drawing

function Color([string]$Hex) {
    return [System.Drawing.ColorTranslator]::FromHtml($Hex)
}

function Set-Rect([System.Drawing.Bitmap]$Bitmap, [int]$X, [int]$Y, [int]$Width, [int]$Height, [System.Drawing.Color]$Fill) {
    for ($yy = $Y; $yy -lt ($Y + $Height); $yy++) {
        for ($xx = $X; $xx -lt ($X + $Width); $xx++) {
            if ($xx -ge 0 -and $xx -lt $Bitmap.Width -and $yy -ge 0 -and $yy -lt $Bitmap.Height) {
                $Bitmap.SetPixel($xx, $yy, $Fill)
            }
        }
    }
}

function Paint-Cell([System.Drawing.Bitmap]$Bitmap, [int]$X, [int]$Y, [System.Drawing.Color]$Fill, [System.Drawing.Color]$Shadow, [System.Drawing.Color]$Highlight) {
    Set-Rect $Bitmap $X $Y 16 16 $Fill
    Set-Rect $Bitmap $X ($Y + 14) 16 2 $Shadow
    Set-Rect $Bitmap ($X + 1) $Y 14 1 $Highlight
    Set-Rect $Bitmap $X $Y 1 16 $Shadow
    Set-Rect $Bitmap ($X + 15) $Y 1 16 $Shadow
    Set-Rect $Bitmap ($X + 3) ($Y + 4) 5 1 $Highlight
    Set-Rect $Bitmap ($X + 10) ($Y + 9) 3 1 $Shadow
}

function Paint-Window([System.Drawing.Bitmap]$Bitmap, [int]$X, [int]$Y, [System.Drawing.Color]$Glass, [System.Drawing.Color]$Reflection, [System.Drawing.Color]$Frame) {
    Set-Rect $Bitmap $X $Y 16 16 $Frame
    Set-Rect $Bitmap ($X + 2) ($Y + 2) 12 11 $Glass
    Set-Rect $Bitmap ($X + 3) ($Y + 3) 8 2 $Reflection
    Set-Rect $Bitmap ($X + 2) ($Y + 12) 12 1 $Frame
    Set-Rect $Bitmap ($X + 11) ($Y + 3) 2 8 $Frame
}

function Paint-Panel([System.Drawing.Bitmap]$Bitmap, [int]$X, [int]$Y, [int]$Width, [int]$Height, [System.Drawing.Color]$Fill, [System.Drawing.Color]$Shadow, [System.Drawing.Color]$Highlight) {
    Set-Rect $Bitmap $X $Y $Width $Height $Fill
    Set-Rect $Bitmap $X ($Y + $Height - 2) $Width 2 $Shadow
    Set-Rect $Bitmap $X $Y $Width 1 $Highlight
    Set-Rect $Bitmap $X $Y 1 $Height $Shadow
    Set-Rect $Bitmap ($X + $Width - 1) $Y 1 $Height $Shadow
}

function Paint-Texture([pscustomobject]$Spec) {
    $bmp = [System.Drawing.Bitmap]::new(64, 64)
    $base = Color $Spec.Base
    $dark = Color $Spec.Dark
    $light = Color $Spec.Light
    $accent = Color $Spec.Accent
    $glass = Color $Spec.Glass
    $metal = Color $Spec.Metal
    Set-Rect $bmp 0 0 64 64 $dark

    # Stable UV zones: body, cab, roof, metal, lights, side windows and windscreen.
    # Keeping each zone coherent prevents large cuboid faces from becoming noisy.
    Paint-Panel $bmp 0 0 32 16 $base $dark $light
    Paint-Panel $bmp 0 16 32 16 $base $dark $light
    Paint-Panel $bmp 0 32 32 16 $dark $dark $light
    Paint-Panel $bmp 32 0 16 16 $metal $dark $light
    Paint-Panel $bmp 48 0 16 16 $light $dark $light
    Paint-Panel $bmp 32 16 16 8 $metal $dark $light
    Paint-Panel $bmp 32 24 16 8 $metal $dark $light
    Paint-Window $bmp 16 40 $glass $light $dark
    Paint-Window $bmp 16 48 $glass $light $dark
    Paint-Window $bmp 32 32 $glass $light $dark
    Set-Rect $bmp 48 16 16 16 $base
    Set-Rect $bmp 48 32 16 16 $dark
    Paint-Panel $bmp 0 48 16 16 $dark $dark $light

    switch ($Spec.Id) {
        'delivery_van' {
            Set-Rect $bmp 0 14 32 2 $light; Set-Rect $bmp 0 28 32 2 $accent
            Set-Rect $bmp 16 16 16 2 $accent
            Set-Rect $bmp 48 48 4 12 $accent; Set-Rect $bmp 52 52 8 4 $light
        }
        'pickup_truck' {
            Set-Rect $bmp 0 14 32 2 $light; Set-Rect $bmp 16 32 16 4 $dark
            Set-Rect $bmp 34 34 12 2 $metal; Set-Rect $bmp 48 48 12 3 $accent
        }
        'roadster' {
            Set-Rect $bmp 0 14 32 2 $light; Set-Rect $bmp 16 32 16 3 $glass
            Set-Rect $bmp 34 34 12 2 $accent; Set-Rect $bmp 48 48 12 2 $light
        }
        'village_bus' {
            Set-Rect $bmp 0 16 16 2 $accent; Set-Rect $bmp 16 16 16 2 $accent
            Set-Rect $bmp 48 16 16 2 $accent; Set-Rect $bmp 48 48 16 3 $metal
        }
        'fire_engine' {
            Set-Rect $bmp 0 16 16 4 $accent; Set-Rect $bmp 16 16 16 4 $accent
            Set-Rect $bmp 32 32 16 2 $metal; Set-Rect $bmp 48 48 5 5 $light
            Set-Rect $bmp 55 48 5 5 $light
        }
        'ambulance' {
            Set-Rect $bmp 0 16 16 3 $accent; Set-Rect $bmp 16 16 16 3 $accent
            Set-Rect $bmp 0 32 16 2 $accent; Set-Rect $bmp 32 32 16 2 $accent
            Set-Rect $bmp 53 51 3 10 $accent; Set-Rect $bmp 50 54 10 3 $accent
        }
        'mining_dump_truck' {
            Set-Rect $bmp 0 32 16 5 $accent; Set-Rect $bmp 16 32 16 5 $accent
            Set-Rect $bmp 32 48 16 4 $dark; Set-Rect $bmp 48 48 16 3 $light
        }
        'farm_harvester' {
            Set-Rect $bmp 0 16 16 3 $light; Set-Rect $bmp 16 16 16 3 $accent
            Set-Rect $bmp 32 32 16 4 $accent; Set-Rect $bmp 48 48 16 3 $dark
        }
        'dune_buggy' {
            Set-Rect $bmp 0 0 16 3 $accent; Set-Rect $bmp 16 0 16 3 $accent
            Set-Rect $bmp 32 0 16 2 $metal; Set-Rect $bmp 48 48 16 3 $accent
        }
        'motorbike' {
            Set-Rect $bmp 0 16 16 3 $accent; Set-Rect $bmp 16 16 16 3 $light
            Set-Rect $bmp 32 32 16 3 $metal; Set-Rect $bmp 48 48 16 3 $accent
        }
    }

    $out = Join-Path $OutputRoot ($Spec.Id + '.png')
    $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

$specs = @(
    [pscustomobject]@{Id='delivery_van'; Base='#D7B27A'; Dark='#6D4C35'; Light='#F0D6A3'; Accent='#3F7F8F'; Glass='#263F4A'; Metal='#A7A7A0'},
    [pscustomobject]@{Id='pickup_truck'; Base='#6F8F57'; Dark='#30452D'; Light='#A7B66B'; Accent='#B85C38'; Glass='#253C45'; Metal='#8C8C82'},
    [pscustomobject]@{Id='roadster'; Base='#3F6FA8'; Dark='#1F365B'; Light='#75A9D6'; Accent='#D2A93A'; Glass='#162B3A'; Metal='#B8C0C3'},
    [pscustomobject]@{Id='village_bus'; Base='#C8A84D'; Dark='#644B24'; Light='#E5D58A'; Accent='#6E8E51'; Glass='#27424B'; Metal='#8B8B7E'},
    [pscustomobject]@{Id='fire_engine'; Base='#A83E32'; Dark='#5A2422'; Light='#D76B48'; Accent='#E2B84B'; Glass='#263A3D'; Metal='#BFC0B6'},
    [pscustomobject]@{Id='ambulance'; Base='#D9D5C8'; Dark='#77766F'; Light='#F5F0DD'; Accent='#B23B36'; Glass='#29424B'; Metal='#B9B9AF'},
    [pscustomobject]@{Id='mining_dump_truck'; Base='#C79A32'; Dark='#634A1B'; Light='#E6C35E'; Accent='#E1B43B'; Glass='#29363A'; Metal='#8D8D80'},
    [pscustomobject]@{Id='farm_harvester'; Base='#5D873F'; Dark='#2F4C2A'; Light='#9EAF54'; Accent='#D38B32'; Glass='#263E3F'; Metal='#9D9D8E'},
    [pscustomobject]@{Id='dune_buggy'; Base='#B36E38'; Dark='#56351F'; Light='#D7A35C'; Accent='#3C8A62'; Glass='#253B3B'; Metal='#A9A99D'},
    [pscustomobject]@{Id='motorbike'; Base='#6C4A8D'; Dark='#322245'; Light='#9A72B5'; Accent='#C7493B'; Glass='#1E2D39'; Metal='#BFC2BC'}
)

New-Item -ItemType Directory -Force -Path $OutputRoot | Out-Null
foreach ($spec in $specs) { Paint-Texture $spec }
