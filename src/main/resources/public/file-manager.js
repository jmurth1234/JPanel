var currentDir = "";
var editor;

$(document).ready(function () {
    $("#save-btn").hide();
    $.ajax({
        url: "/file/",
        success: function (result) {
            var result = JSON.parse(result);
            var fileList = $("#file-list");
            for (var i = 0; i < result.folders.length; i++) {
                var folder = {"folder": result.folders[i]};
                fileList.append(tmpl("item_tmpl", folder));
            }
            for (var i = 0; i < result.files.length; i++) {
                var file = {"file": result.files[i]};
                fileList.append(tmpl("file_tmpl", file));
            }
        }});
});

function loadCurrentDir() {
	$.ajax({
		url: "/file/" + currentDir,
		success: function (result) {
			var result = JSON.parse(result);
			var fileList = $("#file-list");
			fileList.html("");
			var folder = {"folder": ".."};
			fileList.append(tmpl("item_tmpl", folder));
			for (var i = 0; i < result.folders.length; i++) {
				var folder = {"folder": result.folders[i]};
				fileList.append(tmpl("item_tmpl", folder));
			}
			for (var i = 0; i < result.files.length; i++) {
				var file = {"file": result.files[i]};
				fileList.append(tmpl("file_tmpl", file));
			}
		}});
}

function openFolder(folder) {
    if (folder == "..") {
        var folders = currentDir.split("/");
        folders.pop(-1);
        currentDir = folders.join("/");
    } else {
        currentDir += "/" + folder;
    }

    console.log(currentDir);

	loadCurrentDir();

}

$("#upload-btn").change(function(){
	var file = this.files[0];
	var name = file.name;

	var formData = new FormData($('#uploadForm')[0]);
	$.ajax({
		url: "/file/" + currentDir + "/" + name,
		type: 'POST',
		xhr: function() {  // Custom XMLHttpRequest
			var myXhr = $.ajaxSettings.xhr();

			function onProgress(e) {
				$('#upload-btn-label').text("Uploading " + name + ": " + e.loaded);
			}

			if(myXhr.upload) { // Check if upload property exists
				myXhr.upload.addEventListener('progress', onProgress, false); // For handling the progress of the upload
			}
			return myXhr;
		},
		//Ajax events
		success: function () {
			$('#upload-btn-label').text("Upload File");
			alert("File uploaded");
		},
		// Form data
		data: formData,
		//Options to tell jQuery not to process data or worry about content-type.
		cache: false,
		contentType: false,
		processData: false
	});
});

function openFile(file) {
    currentFile = currentDir + "/" + file;

    console.log(currentFile);
    $.ajax({
        url: "/file/" + currentFile,
        success: function (result) {
            $("#save-btn").show();

            editor = ace.edit("editor");
            editor.setTheme("ace/theme/twilight");
            editor.getSession().setMode("ace/mode/yaml");
            editor.setValue(result);
            editor.gotoLine(1);
        }});
}

function saveFile() {
    $.post( "/file/" + currentFile, editor.getValue(), function(result) {
        if (result == 0) {
            alert( "You're not allowed to edit files!" );
        } else {
            alert( "File successfully saved!" );
        }
    });
}


//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//
// H E L P E R    F U N C T I O N S
//
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
/**
 * Function to check if we clicked inside an element with a particular class
 * name.
 *
 * @param {Object} e The event
 * @param {String} className The class name to check against
 * @return {Boolean}
 */
function clickInsideElement( e, className ) {
    var el = e.srcElement || e.target;

    if ( el.classList.contains(className) ) {
        return el;
    } else {
        while ( el = el.parentNode ) {
            if ( el.classList && el.classList.contains(className) ) {
                return el;
            }
        }
    }

    return false;
}

/**
 * Get's exact position of event.
 *
 * @param {Object} e The event passed in
 * @return {Object} Returns the x and y position
 */
function getPosition(e) {
    var posx = 0;
    var posy = 0;

    if (!e) var e = window.event;

    if (e.pageX || e.pageY) {
        posx = e.pageX;
        posy = e.pageY;
    } else if (e.clientX || e.clientY) {
        posx = e.clientX + document.body.scrollLeft + document.documentElement.scrollLeft;
        posy = e.clientY + document.body.scrollTop + document.documentElement.scrollTop;
    }

    return {
        x: posx,
        y: posy
    }
}

/**
 * Variables.
 */
var contextMenuClassName = "context-menu";
var contextMenuItemClassName = "context-menu__item";
var contextMenuLinkClassName = "context-menu__link";
var contextMenuActive = "context-menu--active";

var taskItemClassName = "collection-item";
var taskItemInContext;

var clickCoords;
var clickCoordsX;
var clickCoordsY;

var menu = document.querySelector("#context-menu");
var menuItems = menu.querySelectorAll(".context-menu__item");
var menuState = 0;
var menuWidth;
var menuHeight;
var menuPosition;
var menuPositionX;
var menuPositionY;

var windowWidth;
var windowHeight;

/**
 * Initialise our application's code.
 */
function init() {
    contextListener();
    clickListener();
    keyupListener();
    resizeListener();
}

/**
 * Listens for contextmenu events.
 */
function contextListener() {
    document.addEventListener( "contextmenu", function(e) {
        taskItemInContext = clickInsideElement( e, taskItemClassName );

        if ( taskItemInContext ) {
            e.preventDefault();
            toggleMenuOn();
            positionMenu(e);
        } else {
            taskItemInContext = null;
            toggleMenuOff();
        }
    });
}

/**
 * Listens for click events.
 */
function clickListener() {
    document.addEventListener( "click", function(e) {
        var clickeElIsLink = clickInsideElement( e, contextMenuLinkClassName );

        if ( clickeElIsLink ) {
            e.preventDefault();
            menuItemListener( clickeElIsLink );
        } else {
            var button = e.which || e.button;
            if ( button === 1 ) {
                toggleMenuOff();
            }
        }
    });
}

/**
 * Listens for keyup events.
 */
function keyupListener() {
    window.onkeyup = function(e) {
        if ( e.keyCode === 27 ) {
            toggleMenuOff();
        }
    }
}

/**
 * Window resize event listener
 */
function resizeListener() {
    window.onresize = function(e) {
        toggleMenuOff();
    };
}

/**
 * Turns the custom context menu on.
 */
function toggleMenuOn() {
    if ( menuState !== 1 ) {
        menuState = 1;
        menu.classList.add( contextMenuActive );
    }
}

/**
 * Turns the custom context menu off.
 */
function toggleMenuOff() {
    if ( menuState !== 0 ) {
        menuState = 0;
        menu.classList.remove( contextMenuActive );
    }
}

/**
 * Positions the menu properly.
 *
 * @param {Object} e The event
 */
function positionMenu(e) {
    clickCoords = getPosition(e);
    clickCoordsX = clickCoords.x;
    clickCoordsY = clickCoords.y;

    menuWidth = menu.offsetWidth + 4;
    menuHeight = menu.offsetHeight + 4;

    windowWidth = window.innerWidth;
    windowHeight = window.innerHeight;

    if ( (windowWidth - clickCoordsX) < menuWidth ) {
        menu.style.left = windowWidth - menuWidth + "px";
    } else {
        menu.style.left = clickCoordsX + "px";
    }

    if ( (windowHeight - clickCoordsY) < menuHeight ) {
        menu.style.top = windowHeight - menuHeight + "px";
    } else {
        menu.style.top = clickCoordsY + "px";
    }
}

/**
 * function that sends an action to the server when a menu item link is clicked
 *
 * @param {HTMLElement} link The link that was clicked
 */
function menuItemListener( link ) {
	currentFile = currentDir + "/" + taskItemInContext.getAttribute("data-filepath");

	console.log( "File type - " + taskItemInContext.getAttribute("data-filetype") + ", File path - " + currentFile);
    console.log( "Link action - " + link.getAttribute("data-action"));

	var file_action = {
		"action": link.getAttribute("data-action"),
		"type": taskItemInContext.getAttribute("data-filetype"),
		"target": currentFile,
	};

	if (file_action.action == "Remove" && !confirm("Are you sure you want to delete this file?")) {
		return;
	}

	if (file_action.action == "Rename") {
		file_action["value"] = prompt("What would you like to rename this file to?", taskItemInContext.getAttribute("data-filepath"));
	}

	if (file_action.action == "Download") {
		// fun
		document.getElementById('download').src = window.location.protocol + "//" + document.domain + ":" + window.location.port + "/file/" + currentFile;
		return;
	}


	$.ajax({
		url: "/files/manager",
		type: 'POST',
		contentType: 'application/json; charset=utf-8',
		dataType: 'json',
		data: JSON.stringify(file_action),
		success: function (result) {
			var res = result;
			if (res.success) {
				alert(file_action.action + " successful!");
			} else {
				alert(res.reason);
			}
		}
	});

	loadCurrentDir();

	toggleMenuOff();
}

/**
 * Run the app.
 */
init();


// Simple JavaScript Templating
// John Resig - http://ejohn.org/ - MIT Licensed
(function(){
    var cache = {};

    this.tmpl = function tmpl(str, data){
        // Figure out if we're getting a template, or if we need to
        // load the template - and be sure to cache the result.
        var fn = !/\W/.test(str) ?
            cache[str] = cache[str] ||
                tmpl(document.getElementById(str).innerHTML) :

            // Generate a reusable function that will serve as a template
            // generator (and which will be cached).
            new Function("obj",
                "var p=[],print=function(){p.push.apply(p,arguments);};" +

                    // Introduce the data as local variables using with(){}
                "with(obj){p.push('" +

                    // Convert the template into pure JavaScript
                str
                    .replace(/[\r\t\n]/g, " ")
                    .split("<%").join("\t")
                    .replace(/((^|%>)[^\t]*)'/g, "$1\r")
                    .replace(/\t=(.*?)%>/g, "',$1,'")
                    .split("\t").join("');")
                    .split("%>").join("p.push('")
                    .split("\r").join("\\'")
                + "');}return p.join('');");

        // Provide some basic currying to the user
        return data ? fn( data ) : fn;
    };
})();

