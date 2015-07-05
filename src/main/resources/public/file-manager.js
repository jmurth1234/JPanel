var currentDir = "";

$(document).ready(function () {
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
    currentFile = currentDir + "/" + file;

    console.log(currentFile);
    $.ajax({
        url: "/file/" + currentFile,
        success: function (result) {
            var editor = ace.edit("editor");
            editor.setTheme("ace/theme/twilight");
            editor.getSession().setMode("ace/mode/yaml");
            editor.setValue(result);
            editor.gotoLine(1);
        }});
}