[![License](https://img.shields.io/github/license/dolbyio-samples/blog-android-audio-recording-examples)](LICENSE)

# Dolby.io Interactivity APIs Demo - Android Interview App
The repository is a sample Android application that demonstrates the integration of Dolby.io's APIs for audio processing in an interview app scenario. 

## Overview
This sample project consists of an Android app and a Node.js server. Together, they facilitate interview practice through one-on-one video conferencing. This project leverages [Dolby.io](dolby.io) Interactivity APIs and SDK to provide conferencing, and uses its [REST APIs](https://dolby.io/developers/interactivity-apis/reference/rest-apis/authentication) to authenticate and monitor the state of the app. 

## Requirements

You will need Android Studio (preferably 4.0 or higher, although 3.6 is the absolute minimum). If you don't have an emulator installed, set that up (instructions [here](https://developer.android.com/studio/run/managing-avds)). You should also have the necessary SDK Platforms - the Android 11.0, API 30 SDK should be fine for testing purposes (from Android Studio, go to Tools > SDK Manager to download).

For the server, all you need are recent versions of Node.js and npm (refer to the package-lock file for more information). 

## Getting Started

Next, clone this repository into a directory of your choice. 

```bash
git clone github.com/dolbyio-samples/blog-android-interview-app
```

Then, in Android Studio, choose to import a project and navigate to where you cloned the project. Choose the android/ directory to open. To be able to fully use the app, you need to have a server running. See the localhost instructions below for a quick way to do some testing, or the more detailed guide below to deploy the server on Heroku. Either way, you'll need to change the URL variable the StringConstants file in the app code before running.  

After you have a server running, select your Android emulator from the drop-down menu at the top of Android Studio and press the green play button to build and install the app. Alternatively, you can connect a physical device via USB and install the app that way. See [this link](https://developer.android.com/studio/debug/dev-options) for information on how to install the app onto your device. 


### Server steps

*Option 1 - Localhost*

Navigate into the server directory and run ```npm install``` to install all the necessary packages and dependencies. 

Next, create a ```.env``` file in the server directory that defines two variables: ```APP_KEY``` and ```APP_SECRET```, and set them equal to your [Dolby.io Interactivity](https://dolby.io/developers/interactivity-apis/overview/introduction) key and secret respectively. 

Still in the server directory, run 
```bash
node ./server.js
```   

to start listening on port 3000. Finally, in the StringConstants class in Android Studio, uncomment the variable labeled for local testing and insert your IP address where indicated. You're ready to run the app! 

*Option 2 - Heroku*

If you want a more permanent way to host a server for access tokens and calls to the Monitor API, you can use Heroku. Full details on how to deploy a Node.js server can be found [here](https://devcenter.heroku.com/articles/deploying-nodejs). 

After making a Heroku account, install the Heroku CLI. For Macs, use 

```
brew install heroku/brew/heroku
```

For Windows, visit the Heroku website to find an installer. Run 

``` 
heroku login
``` 

and enter your Heroku information. For easy deployment, Heroku expects 
your code to be inside a git repository. While you can create a git subtree for the server folder inside this project directory, for simplicity just copy the server directory to a different location and initialize it as a new git repository. 

From inside the root project directory: 
```
cp -r server/ ~/your/path/here
```

Then inside that directory: 

```
git init
``` 

Add a .gitignore file with the typical structure for a Node project:

```
/node_modules
npm-debug.log
.DS_Store
/*.env
```

And add all the server files:

```
git add .
```

Next, create a Procfile, which defines to Heroku which command starts your code. The Procfile should not have a file extension, and should contain the following: 

```
web: npm start
```

Run 

``` 
heroku create
``` 

This creates a Heroku project that is a remote for your local repository. Push your files to the remote with 

```
git push heroku main
``` 

You should see output confirming successful deployment. 

On the Heroku website, your app should now show up on your [dashboard](https://dashboard.heroku.com/apps). In the Settings tab of your app, 
add two config vars: APP_KEY and APP_SECRET, with the values of your Interactivity key and secret respectively. 

Your server is now deployed! Uncomment the URL variable labeled for Heroku in the StringConstants file of the app and enter your Heroku app URL instead of the placeholder text. To find out the URL of your app, you can type 

``` 
heroku open
```

in the directory where your server code lives and copy the resulting URL. 

Happy mock interviewing!

# Report a Bug 
In the case any bugs occur, report it using Github issues, and we will see to it. 

# Forking
We welcome your interest in trying to experiment with our repos. 

# Feedback 
If there are any suggestions or if you would like to deliver any positive notes, feel free to open an issue and let us know!

# Learn More
For a deeper dive, we welcome you to review the following:
 - [Communications API](https://docs.dolby.io/communications-apis/docs)
 - [Getting Started with Android SDK](https://docs.dolby.io/communications-apis/docs/getting-started-with-android)
 - [Build a Podcast Recording App for Android using the Dolby.io Monitor API](https://dolby.io/blog/build-a-podcast-recording-app-for-android-using-the-dolby-io-monitor-api/)
 - [Add Conference Audio Moderation Using Hive.ai and the Dolby.io Android SDK](https://dolby.io/blog/add-conference-audio-moderation-using-hive-ai-and-the-dolby-io-android-sdk/)
 - [Blog Session - Communications API](https://dolby.io/blog/category/communications/)

# About Dolby.io
Dolby.io is a new developer platform by Dolby Laboratories using decades of Dolby's sight and sound technology. Learn how to integrate APIs for real-time streaming, communications, and media processing solutions. You can find the tools, documentation, and sample projects to help you get started.

<div align="center">
  
[![Dolby.io Home](https://img.shields.io/badge/-HomePage-yellowgreen)](https://dolby.io/)
&nbsp; &nbsp; &nbsp;
[![Dolby.io Documentation](https://img.shields.io/badge/-Our%20Documentation-orange)](https://docs.dolby.io/)
&nbsp; &nbsp; &nbsp;

[![@dolbyio on LinkedIn](https://img.shields.io/badge/linkedin-%230077B5.svg?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/company/dolbyio)
&nbsp; &nbsp; &nbsp;
[![@dolbyio on YouTube](https://img.shields.io/badge/YouTube-FF0000?style=for-the-badge&logo=youtube&logoColor=white)](https://www.youtube.com/@DolbyIO/)
&nbsp; &nbsp; &nbsp;
[![@dolbyio on Twitter](https://img.shields.io/badge/Twitter-%231DA1F2.svg?style=for-the-badge&logo=Twitter&logoColor=white)](https://twitter.com/DolbyIO/)
&nbsp; &nbsp; &nbsp;
  
</div>
