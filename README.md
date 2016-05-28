# JPanel
A plugin based control panel for Bukkit / Spigot http://dev.bukkit.org/bukkit-plugins/jpanel/
![Screenshot](http://dev.bukkit.org/media/images/84/90/Screenshot_from_2015-06-25_16-13-15.png)

### What is this?
JPanel is a plugin based control panel for your minecraft server. It is viewable within a web browser, and does not require running any external web servers, or knowledge of PHP or databases. This plugin is in beta, and has a number of key features, including:

* A live view of the server console
* Live view of the server ram usage, cpu usage and ticks per second
* User permissions
* Modify player groups (requires vault)
* Easily kick or ban online players (not currently implemented in new player manager)
* Manage server files
* Passwords hashed with a salt

### Before installing
THIS PLUGIN REQUIRES JAVA 8. ALSO BE AWARE IT'S STILL IN BETA, AND THERE MIGHT BE BUGS OR INCOMPLETE FEATURES.

### Installing
Since this plugin doesn't require anything else other than the Bukkit server, installation is extremely easy

1. Put the plugin jar file in your plugins folder
2. Restart the server
3. Ensure that ports 4567 and 9003 are port forwarded (this can be tested with [this tool here](http://www.canyouseeme.org/))
4. In the server console, use /addlogin <username> <password> to add a user to the panel
5. OPTIONAL: Stop the server and edit the config file to allow you access to the JPanel features

### For Developers
As of JPanel Beta 5, you can now add pages to the panel. For more information, please [see the guide](http://dev.bukkit.org/bukkit-plugins/jpanel/pages/guide-to-add-pages/).

### Commands
All commands can only be executed through the console, either via JPanel or a standard minecraft console.

* /addlogin <username> <passsword> - allows you to add a user to a panel
* /passwd <username> <oldpassword> <newpassword> - change the password of a user

### Panel Permissions
These are modified through the config file. Once modified, restart the server.
* canEditFiles - allows a JPanel user to edit files
* canChangeGroups - allows a JPanel user to change the groups of a user
* canSendCommands - allows a JPanel user to send commands through the console

### Non browser Clients
I'm developing a series of apps that you can use to access the panel. Currently, there is an app for windows 10.

* Windows 10 App - [https://www.microsoft.com/en-us/store/apps/jpanel/9nblggh1rnm5](https://www.microsoft.com/en-us/store/apps/jpanel/9nblggh1rnm5) (source will be released later)
* Android app - coming soon!
* iPhone app - due to a lack of $99 a year, a Mac computer and a device to test on, this is unlikely to be coming any time soon

### Source Code
All source code is avaliable on [GitHub](https://github.com/rymate1234/JPanel), check it out!
