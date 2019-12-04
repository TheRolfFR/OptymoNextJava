# How I reverse engineered my bus stop service to make my own app : Part 1 : Get the data.

## Introduction 

I live in Belfort and I do use the bus service [Optymo](https://www.optymo.fr) daily and need to continuously check fastly when the next bus will be coming. However, nothing but a big PDF is given to us to know it. What I needed is a way to check instantly from my notifications the next buses.

Unfortunately, the company Optymo has no API available. I needed to find a way to recover informations such as the next stops, all the stops names and all the lines with the stops.
<br><br>
## The little QR Codes

QR Codes are available to check the next buses at a given stop. So let's use it. The most interesting data of the web page of this QR code is its URL. It contains some sort of slug telling what is the stop name and an index to know what line it is. However the slug is very irregular and depends a lot of the stop name which can be complicated sometimes.

![qrcode_stop.png](https://therolf.fr/anchor/content/qrcode_stop.png)

Ex: this url [https://siv.optymo.fr/passage.php?ar=Utb01](https://siv.optymo.fr/passage.php?ar=Utb01) which slug is **Utb01**, has a name which is **Techn'hom 1/UTBM**

I cannot spend weeks searching each slug for each stop and line. I need to find another solution.

## The map

The Optymo website has something really interesting. It has a Google map with all the stops integrated ([link](https://www.optymo.fr))! But where did he find its stops? let's open the browser network traffic tool :

![network_traffic_firefox_bus.gif](https://therolf.fr/anchor/content/network_traffic_firefox_bus.gif)

Wow, that's a lot of traffic! Hard to know what is what but the domain names might help me.

<br>
This sounds cool : "interactive map"?

![optymo carte interactive.png](https://therolf.fr/anchor/content/optymo-carte-interactive.png)

The bus positions in real time ? Not interesting for me.
```javascript
var srcFileBus = modePrm == "demo" ? "itrsub/get_markers_demo.php" :
					modePrm == "vide" ? "itrsub/get_markers_vide.php" :
						modePrm == "urb" ? "itrsub/get_markers_urb.php" :
							modePrm == "sub" ? "itrsub/get_markers_sub.php" :
								"itrsub/get_markers_urb.php";
```

<br>

What is this script? belfort.js ? There are some API keys, let's see what I can do with it.
![api_key.png](https://therolf.fr/anchor/content/api_key.png)

<br>

stopsuggestionengine.js ? That's what I am looking for!

![stops_suggestion.png](https://therolf.fr/anchor/content/stops_suggestion.png)
Nice! There is a URL that use the API slug to get the stops!

`"url" : '//app.mecatran.com/' + apiPath + instance + '?includeStops='+ includeStops +'&includeStations='+ includeStations +'&apiKey=' + apiKey`

<br> Now let's get them! What it this thing?

![stops_bus_xml.gif](https://therolf.fr/anchor/content/stops_bus_xml.gif)

That might be XML. Let's check the source code of the page. Now this is better!

![stop_xml.png](https://therolf.fr/anchor/content/stop_xml.png)

Yes! Let's download it. I will parse it later to get everything I need. Now I need to know the next bus for a given stop. When I click on a bus stop a tooltip is displayed :

![stop-tooltip.png](https://therolf.fr/anchor/content/stop-tooltip.png)

This page looks like the one from the QR code but this one works for all the stops in the same location. The URL may be different. I'll see what it is with the code inspector. Hello my friend!

![iframe_stop.png](https://therolf.fr/anchor/content/iframe_stop.png)

[https://siv.optymo.fr/passage.php?ar=technhom1utbm&type=1](https://siv.optymo.fr/passage.php?ar=technhom1utbm&type=1)
 That's a win! The URL slug is much easier to guess. It is only alphanumeric characters in lowercase. Now that I have all my URLs and my stops I can start working on the data model for my app. I will be using Android Studio and Java to do it. First I will make a Java desktop project to organize all my data and see how I can achieve my goals.

Thanks for reading.
See you later for the next part.