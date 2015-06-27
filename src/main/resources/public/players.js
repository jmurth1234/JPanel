function kickPlayer(element) {
    $.ajax({url: "/player/" + $(element).attr("name") + "/kick"});
}

function banPlayer(element) {
    $.ajax({url: "/player/" + $(element).attr("name") + "/ban"});
}