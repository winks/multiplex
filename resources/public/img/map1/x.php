<?php
$dir = dir(".");

while ($d = $dir->read()) {
    if (substr($d, -3) !== "png") { continue; }
    printf('<img src="%s" />', $d);
}
