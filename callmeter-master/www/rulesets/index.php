<?

if (array_key_exists('out', $_GET)) {
  $out = $_GET['out'];
} else {
  $out = '';
}

if ($out == 'json') {
  header('Content-type: application/json');
  print file_get_contents($location.'rulesets.json');
  exit;
} else {
  $json = json_decode(file_get_contents($location.'rulesets.json'), true);
}

function endsWith($haystack, $needle) {
  $length = strlen($needle);
  $start  = $length * -1; //negative
  return (substr($haystack, $start) === $needle);
}


function _value_in_array($array, $find){
  if(!is_array($array)){
    return FALSE;
  }
  foreach ($array as $key => $value) {
    if($find == $value){
      return TRUE;
    }
  }
  return FALSE;
}

$isAndroid = preg_match('/Android/', $_SERVER['HTTP_USER_AGENT']);
$location = './';

?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<link rel="stylesheet" type="text/css" href="/default.css" />
<link rel="stylesheet" type="text/css" href="common.css" />
<?
  if ($isAndroid) {
    echo '<link rel="stylesheet" type="text/css" href="android.css" />' . "\n";
  } else {
    echo '<link rel="stylesheet" type="text/css" href="standard.css" />' . "\n";
  }
?>
<title>Call Meter 3G rule sets</title>
<script type="text/javascript">
  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', 'UA-25757356-1']);
  _gaq.push(['_trackPageview']);

  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  })();
</script>
<script src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
<script src="http://cdn.jquerytools.org/1.2.7/full/jquery.tools.min.js"></script>
<script src="jquery.lazyload.min.js" type="text/javascript"></script>
<script type="text/javascript">
  window.onload = function() {

    var overlayconfig = {
      mask: { opacity: 0.8},
      onLoad: function() {
        $("img.lazyoverlay").lazyload({ effect: "fadeIn"});
      }
    };

    $("img[rel]").overlay(overlayconfig);
    $("a[rel]").overlay(overlayconfig);

    $("img.lazy").lazyload({ effect: "fadeIn"});

    var eCountry = document.getElementById('country');
    var eProvider = document.getElementById('provider');
    var f = function() {
      var u;
      if (window.location.search) {
        u=window.location.href.replace(/country=[^&]*/, '').replace(/provider=[^&]*/, '');
        if (eCountry.value) {
          u=u + '&country=' + eCountry.value
        }
        if (eProvider.value) {
          u=u + '&provider=' + eProvider.value;
        }
      } else {
        u=window.location.href
        if (eCountry.value) {
          u=u + '?country=' + eCountry.value
          if (eProvider.value) {
            u=u + '&provider=' + eProvider.value;
          }
        } else if (eProvider.value) {
          u=u + '?provider=' + eProvider.value;
        }
      }
      u=u.replace('?&', '?').replace('??', '?');
      window.location.href=u;
    }
    eCountry.onchange = f;
    eProvider.onchange = f;
  }
</script>
</head>
<body>

<div class="top">
<ul>
<li><a href="/">ub0r apps</a></li>
<li>&gt; <a href="/android/callmeter/">Call Meter 3G</a></li>
<li>&gt; <a href="/android/callmeter/rulesets/">rule sets</a></li>
</ul>
<span class="topright"><a href="/contact.html">contact</a></span>
</div>

<?

if (array_key_exists('country', $_GET)) {
  $countryfilter = $_GET['country'];
} else {
  $countryfilter = '';
}
if (array_key_exists('provider', $_GET)) {
  $providerfilter = $_GET['provider'];
} else {
  $providerfilter = '';
}

$country = array();
$countries = array_keys($json);
$common = array_pop($countries);
array_unshift($countries, $common);

$providers = array();
$files = array();
$importurls = array();
$provider = array();
$title = array();
$link = array();
$description = array();
$longdescription = array();
foreach ($countries as $c) {
  $lst = $json[$c];
  foreach ($lst as $ruleset) {
    // var_dump($ruleset);
    $f = preg_replace('/http:..*\//', '', $ruleset['importurl']);
    array_push($files, $f);

    $p = $ruleset['provider'];
    if (!_value_in_array($providers, $p) && !empty($c) && (!$countryfilter || $countryfilter == $c)) {
      $providers[] = $p;
    }

    $country[$f] = $c;
    $provider[$f] = $p;
    $importurls[$f] = $ruleset['importurl'];
    $title[$f] = $ruleset['title'];
    if (array_key_exists('link', $ruleset)) {
      $link[$f] = $ruleset['link'];
    }
    if (array_key_exists('description', $ruleset)) {
      $description[$f] = $ruleset['description'];
    }
    if (array_key_exists('longdescription', $ruleset)) {
      $longdescription[$f] = $ruleset['longdescription'];
    }
  }
}


sort($providers);

?>

<div id="content">

<h1>Import rule sets</h1>

<?

if ($isAndroid) {
  echo 'Import a rule set of your choice directly to your <a href="https://market.android.com/details?id=de.ub0r.android.callmeter">Call Meter 3G</a> simply by selecting its link below. To import the very same rule set on an other device, you should just open the barcode and scan it with a barcode scanner app (e.g. <a href="https://market.android.com/details?id=com.google.zxing.client.android">Barcode Scanner</a>).' . "<br />\n";
} else {
  echo 'Import a rule set of your choice directly to your <a href="https://market.android.com/details?id=de.ub0r.android.callmeter">Call Meter 3G</a> simply by scanning the barcode with a barcode scanner app (e.g. <a href="https://market.android.com/details?id=com.google.zxing.client.android">Barcode Scanner</a>).' . "<br />\n";
}

echo 'You might need to edit your very own limit or cost settings after importing the rule set.' . "<br />\n";
echo '<br />If you want your rule set shown here, you just need to export it to me from within the app.';
?>

<h2>Table of Rule sets</h2>
<form>
<table>
<tr class="thead">
<th><select id="country" name="country">
<option value="">Country</option>
<option value=""></option>
<?

foreach ($countries as $c) {
  if ($c && $c == $countryfilter) {
    echo '<option value="'.$c.'" selected="selected">'.$c.'</option>';
  } else {
    echo '<option value="'.$c.'">'.$c.'</option>';
  }
}
?>
</select></th>
<th><select id="provider" name="provider">
<option value="">Provider</option>
<option value=""></option>
<?
foreach ($providers as $p) {
  if ($p && $p == $providerfilter) {
    echo '<option value="'.$p.'" selected="selected">'.$p.'</option>';
  } else {
    echo '<option value="'.$p.'">'.$p.'</option>';
  }
}
?>

</select></th>
<th>Plan</th>
<th>
<? if ($isAndroid) { echo "Link"; } else { echo "Barcode"; } ?>
</th>
</tr>

<?
foreach ($files as $f) {
  if (!empty($countryfilter) && $countryfilter != $country[$f]) {
    continue;
  }
  if (!empty($providerfilter) && $providerfilter != $provider[$f]) {
    continue;
  }
  $ff = str_replace('.xml', '', $f);
  $ff = str_replace('.', '', $ff);
  $importurl = $importurls[$f];
  $chl = urlencode($importurl);
  $hash = (hexdec(hash('crc32', $f)) % 10);
  $barcodeurl_s = 'http://' . $hash . '.chart.apis.google.com/chart?chs=100x100&amp;cht=qr&amp;chl=' . $chl;
  $barcodeurl_l = 'http://' . $hash . '.chart.apis.google.com/chart?chs=400x400&amp;cht=qr&amp;chl=' . $chl;
  echo "<tr class=\"trow\" id=\"f_".$ff."\">\n";
  echo '<td><a href="?country='.$country[$f].'">' . $country[$f] . "</a></td>\n";
  if (empty($provider[$f])) {
    echo "<td></td>\n";
  } else {
    echo '<td><a href="?country='.$country[$f].'&provider='.$provider[$f].'">' . $provider[$f] . "</a></td>\n";
  }
  echo '<td><a href="#f_'.$ff.'">' . $title[$f].'</a>';
  if (array_key_exists($f, $description)) {
    echo '<br />'.nl2br($description[$f])."\n";
  }
  if (array_key_exists($f, $link) || array_key_exists($f, $longdescription)) {
    echo '<br />'."\n";
    if (array_key_exists($f, $link)) {
      echo '<a href="'.$link[$f].'">Additional Link</a> '."\n";
    }
    if (array_key_exists($f, $longdescription)) {
      echo '<a href="#f_'.$ff.'" rel="#descr_'.$ff.'">Additional Information</a> '."\n";
      // overlay
      echo '<div class="overlay" id="descr_'.$ff.'">';
      echo '<div class="details">';
      echo nl2br($longdescription[$f]);
      echo '</div>';
      echo '</div>';
    }
  }
  echo '</td>'."\n";
  echo '<td>';
  if ($isAndroid) {
    echo 'import:<br />';
    echo '<a href="' . $importurl . '">link</a>' . "\n";
    echo '<br />';
    echo '<a href="' . $barcodeurl_l . '">barcode</a>' . "\n";
  } else {
    echo '<div class="barcode">';
    echo '<img class="lazy" src="white.gif" data-original="' . $barcodeurl_s . '" rel="#barcode_'.$ff.'" />';
    echo '</div>'."\n";
    // overlay
    echo '<div class="overlay" id="barcode_'.$ff.'">';
    echo '<img class="lazyoverlay" height="400" width="400" src="white.gif" data-original="' . $barcodeurl_l . '" />';
    echo '</div>'."\n";
  }
  echo '</td>';
  echo "</tr>\n";
  echo "\n";
}
?>

</table>
</form>
</div>
</body>
</html>

