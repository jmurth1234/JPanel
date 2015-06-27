function kickPlayer(element) {
    var player = $(element).attr("name");
    var confirmation = confirm("Are you sure you want to kick " + player + "?");
    if (confirmation == true) {
        $.ajax({url: "/player/" + player + "/kick"});
    }
}

function banPlayer(element) {
    var player = $(element).attr("name");
    var confirmation = confirm("Are you sure you want to ban " + player + "?");
    if (confirmation == true) {
        $.ajax({url: "/player/" + player + "/ban"});
    }
}