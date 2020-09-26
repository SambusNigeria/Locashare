## Introduction

Locashare is an Android location sharing application built using Esri's ArcGIS [Runtime SDK](https://developers.arcgis.com/arcgis-runtime/) and [REST API](https://developers.arcgis.com/rest/). It tracks the users location and provides an implicit sharing intent to share the location using.

## Usage

This source demonstrates how to display a map, track the user's location and get the location attributes on the fly as it updates. You can use this source to deploy a location tracker for instance, if you want to track your workers in real-time, or if you just want to know how it's done. It can also be used to start off a bigger project. 

## Attribution

You're free to use this source or link to it for any purpose.

## Contribution

You're also welcome to contribute to this source, just fork the repo, create a branch, commit your changes and make a pull request.

## License

The Laravel framework is open-sourced software licensed under the [MIT license](http://opensource.org/licenses/MIT).

## How to build this sample

You can use any of the options below to 
- You can download the zip and extract it, then open in [Android Studio IDE](https://developer.android.com/studio).
- You can also checkout from version control and provide the repository address on Android Studio.

## Breakdown of the components

To create this application from scratch, you can follow the following procedure. When you build this application, your map will contain a watermark that says "For Developer Use Only". 
The unlicensed code is only to be used for development and testing purposes. To remove the watermark and use the SDK in live projects, you need to get an ArcGIS Developer Runtime Standard License at least. 
This will enable you to connect your applications to your license and use in live projects. 
For more information on how to get a license, please visit our website https://sambusgeospatial.com or send us a mail at info*sambusgeospatial.com

1. ### Create a new project 
Create a new Android Application in Android studio using the Basic Activity template. This will create the needed files and directories you need.
1. ### Add the ArcGIS component from bintray
Open your project-level build.gradle file and add the following lines to the repository section:
```groovy
---
allprojects {
    repositories {
        ---
        maven {
            url 'https://esri.bintray.com/arcgis'
        }
    }
}
---
```
1. ### Add the ArcGIS runtime and Volley dependencies
Open your module level build.gradle file and append the following lines to the dependencies section:
```groovy
dependencies {
    ---
    implementation 'com.android.volley:volley:1.1.1'
    implementation 'com.esri.arcgisruntime:arcgis-android:100.7.0'
    ---
}
```
The volley dependency is to enable you make network requests to the REST API

1. ### Add ABI Filters
This is used to specify the ndk versions you want in the release build. This can help to greatly reduce your APK size.
Still in the module level build.gradle file, add the lines below in the buildTypes subsection of the android section:
```groovy
---
android {
    ---
    buildTypes {
        release {
            ---
            ndk {
                abiFilters "armeabi-v7a", "arm64-v8a"
            }
        }
    }
    ---
}
```
Synchronize your project files with gradle after making these changes.

## Finishing up
The instructions in the Esri Lab can be followed for more detailed explanations.
The only thing not covered in the lab that was used in this source is the ability to get the user's coordinates, reverse geocode it to get the address and display in a TextView and used in other parts of the application.
Some times, we want to display the coordinates of the user or send it to our server and store. 
To do this, you need to call the addDrawStatusChangedListener() method of the MapView as seen below, then do whatever you want with the location attributes obtained from the getLocationDisplay().getLocation().getPosition() methods as shown below:
```Java
            mMapView.addDrawStatusChangedListener(drawStatusChangedEvent -> {
                Point point = new Point(mMapView.getLocationDisplay().getLocation().getPosition().getX(), mMapView.getLocationDisplay().getLocation().getPosition().getY());
                updateLocation(point);
            });
```
Here, I used the x and y to create a point and send the point to a method which I use to update the TextViews and also reverse geocode the coordinates to get the address.
The Check the content_main.xml and MainActivity.java in this repository and modify yours accordingly.
Also copy the AuthenticationInteractor.java file to your package directory. This is the class used for the volley implementation.
Make sure there are no errors and run your project.
If all things work well, you can see the screenshot below and clicking on the share button will open the sharing window.
MainActivity:
<image alt="MainActivity" src="https://sambusgeospatial.maps.arcgis.com/sharing/rest/content/items/3474c5069efc4f399b115bbd36a0a179/data" width="300px" />
Sharing:
<image alt="Sharing" src="https://sambusgeospatial.maps.arcgis.com/sharing/rest/content/items/23b81df51c91488dad1344802b10aa5b/data" width="300px" />

Other things used in this project include:
- Material Design
- Constraint layout
- AndroidX
- others you can find out in the app level build.gradle file

## Support
If you have any issues following these steps, or need technical support, you can reach out to me via boke*sambusgeospatial.com

Sending emails?
*Replace * with an @* 

