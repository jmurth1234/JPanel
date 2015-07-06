var currentDir = "";
var editor;

$(document).ready(function () {
    $("#save-btn").hide();
    $.ajax({
        url: "/file/",
        success: function (result) {
            var result = JSON.parse(result);
            var fileList = $("#file-list");
            fileList.append('<div class="collection-item"><h4>Folders</h4></div>');
            for (var i = 0; i < result.folders.length; i++) {
                fileList.append('<a href="#!" onclick="openFolder(this)" class="collection-item">' + result.folders[i] + '</a>');
            }
            fileList.append('<div class="collection-item"><h4>Files</h4></div>');
            for (var i = 0; i < result.files.length; i++) {
                fileList.append('<a href="#!" onclick="openFile(this)" class="collection-item">' + result.files[i] + '</a>');
            }
        }});
});

function openFolder(element) {
    folder = $(element).text();
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
            fileList.append('<div class="collection-item"><h4>Folders</h4></div>');
            fileList.append('<a href="#!" onclick="openFolder(this)" class="collection-item">..</a>');
            for (var i = 0; i < result.folders.length; i++) {
                fileList.append('<a href="#!" onclick="openFolder(this)" class="collection-item">' + result.folders[i] + '</a>');
            }
            fileList.append('<div class="collection-item"><h4>Files</h4></div>');
            for (var i = 0; i < result.files.length; i++) {
                fileList.append('<a href="#!" onclick="openFile(this)" class="collection-item">' + result.files[i] + '</a>');
            }
        }});
}


function openFile(element) {
    file = $(element).text();
    $("#save-btn").show();
    currentFile = currentDir + "/" + file;

    console.log(currentFile);
    $.ajax({
        url: "/file/" + currentFile,
        success: function (result) {
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