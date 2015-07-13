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

function openFolder(folder) {
    if (folder == "..") {
        var folders = currentDir.split("/");
        folders.pop(-1);
        currentDir = folders.join("/");
    } else {
        currentDir += "/" + folder;
    }

    console.log(currentDir);
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

