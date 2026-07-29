param([string]$OutputDirectory = "src/main/resources/assets/unofficialdmzaddon/textures/environment/planets")

Add-Type -AssemblyName System.Drawing
$width = 128
$height = 64
New-Item -ItemType Directory -Force $OutputDirectory | Out-Null

function Noise([double]$u, [double]$v, [int]$seed) {
    $a = [Math]::Sin(($u * 6.283185307 + $seed) * 2.0) * 0.35
    $b = [Math]::Sin(($u * 6.283185307 * 3.0) + ($v * 9.0) + $seed * 0.37) * 0.25
    $c = [Math]::Cos(($u * 6.283185307 * 7.0) - ($v * 13.0) + $seed * 0.19) * 0.18
    $d = [Math]::Sin($v * 21.0 + [Math]::Cos($u * 6.283185307 * 4.0) * 2.0) * 0.12
    return $a + $b + $c + $d
}

function MixColor([int[]]$a, [int[]]$b, [double]$amount) {
    $t = [Math]::Max(0.0, [Math]::Min(1.0, $amount))
    return @(
        [int]($a[0] + ($b[0] - $a[0]) * $t),
        [int]($a[1] + ($b[1] - $a[1]) * $t),
        [int]($a[2] + ($b[2] - $a[2]) * $t),
        255)
}

function WritePlanet([string]$name, [int]$seed, [int[]]$base, [int[]]$feature, [int[]]$accent, [double]$threshold) {
    $bitmap = [System.Drawing.Bitmap]::new($width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        for ($y = 0; $y -lt $height; $y++) {
            $v = $y / [double]($height - 1)
            $latitudeShade = 0.82 + 0.18 * [Math]::Cos(($v - 0.5) * [Math]::PI)
            for ($x = 0; $x -lt $width; $x++) {
                $u = $x / [double]$width
                $noise = Noise $u $v $seed
                if ($noise -gt $threshold) {
                    $color = MixColor $feature $accent ([Math]::Min(1.0, ($noise - $threshold) * 2.3))
                } else {
                    $color = MixColor $base $feature ([Math]::Max(0.0, ($noise - $threshold + 0.28) * 1.4))
                }
                $r = [Math]::Min(255, [int]($color[0] * $latitudeShade))
                $g = [Math]::Min(255, [int]($color[1] * $latitudeShade))
                $b = [Math]::Min(255, [int]($color[2] * $latitudeShade))
                $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $r, $g, $b))
            }
        }
        $bitmap.Save((Join-Path $OutputDirectory "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    } finally { $bitmap.Dispose() }
}

WritePlanet 'earth' 11 @(20,74,160) @(42,126,61) @(218,205,139) 0.08
WritePlanet 'namek' 23 @(55,168,193) @(75,190,72) @(175,224,76) -0.02
WritePlanet 'otherworld' 37 @(242,189,72) @(250,228,151) @(255,250,225) 0.03
WritePlanet 'sacred_kai' 41 @(104,69,153) @(197,112,191) @(245,180,217) -0.05
WritePlanet 'time_chamber' 53 @(215,205,184) @(245,239,220) @(171,153,119) 0.10

$explosion = [System.Drawing.Bitmap]::new($width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
try {
    for ($y = 0; $y -lt $height; $y++) {
        $v = $y / [double]($height - 1)
        for ($x = 0; $x -lt $width; $x++) {
            $u = $x / [double]$width
            $noise = Noise $u $v 97
            $hot = [Math]::Max(0.0, [Math]::Min(1.0, 0.55 + $noise))
            $r = 255
            $g = [int](55 + 195 * $hot)
            $b = [int](8 + 92 * $hot * $hot)
            $alpha = [int](190 + 65 * [Math]::Max(0.0, $noise))
            $explosion.SetPixel($x, $y, [System.Drawing.Color]::FromArgb([Math]::Min(255, $alpha), $r, $g, $b))
        }
    }
    $explosion.Save((Join-Path $OutputDirectory 'planet_explosion.png'), [System.Drawing.Imaging.ImageFormat]::Png)
} finally { $explosion.Dispose() }