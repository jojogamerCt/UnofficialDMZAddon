Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$textureDir = Join-Path $root "src/main/resources/assets/unofficialdmzaddon/textures/gui"
$glyphPath = Join-Path $textureDir "custom_forms_glyph.png"
$outputPath = Join-Path $textureDir "custom_forms_menu.png"
$dmzButtonsPath = Join-Path $root "dragonminez/src/main/resources/assets/dragonminez/textures/gui/buttons/menubuttons.png"

$glyph = [System.Drawing.Bitmap]::FromFile($glyphPath)
$dmzButtons = [System.Drawing.Bitmap]::FromFile($dmzButtonsPath)
$atlas = New-Object System.Drawing.Bitmap 256, 256, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

function Get-NativeFramePixel($buttons, $localX, $localY, $sourceV) {
    # The eight DMZ navigation cells all use the exact same native frame. Their
    # icons differ, so the most frequent ARGB value at each coordinate recovers
    # the original common background without inventing or approximating it.
    $counts = @{}
    foreach ($sourceU in 0, 20, 40, 60, 80, 100, 120, 140) {
        $argb = $buttons.GetPixel($sourceU + $localX, $sourceV + $localY).ToArgb()
        $counts[$argb] = 1 + [int]$counts[$argb]
    }
    $winner = $counts.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 1
    return [System.Drawing.Color]::FromArgb([int]$winner.Key)
}

function Draw-NativeFrame($atlas, $buttons, $glyph, $targetX, $sourceV) {
    for ($y = 0; $y -lt 20; $y++) {
        for ($x = 0; $x -lt 20; $x++) {
            $atlas.SetPixel($targetX + $x, $y, (Get-NativeFramePixel $buttons $x $y $sourceV))
        }
    }
    for ($y = 0; $y -lt $glyph.Height; $y++) {
        for ($x = 0; $x -lt $glyph.Width; $x++) {
            $pixel = $glyph.GetPixel($x, $y)
            if ($pixel.A -gt 0) { $atlas.SetPixel($targetX + 2 + $x, 2 + $y, $pixel) }
        }
    }
}

Draw-NativeFrame $atlas $dmzButtons $glyph 0 0
Draw-NativeFrame $atlas $dmzButtons $glyph 20 20

$atlas.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
$atlas.Dispose()
$dmzButtons.Dispose()
$glyph.Dispose()
