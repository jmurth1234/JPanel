var connected = true;

function refresh() {
    var progressCPU = $("#cpu");
    var textCPU = $("#cpu-text");

    var progressRAM = $("#ram");
    var textRAM = $("#ram-text");

    var progressTPS = $("#tps");
    var textTPS = $("#tps-text");

    if (connected) {
        $.ajax({
            url: "/stats",
            success: function (result) {
                var result = JSON.parse(result);
                progressRAM.attr("class", "determinate");
                progressRAM.css("width", (result.free / result.total) * 100 + "%");
                textRAM.text("RAM: " + Math.round(result.free / 1024) + "MB / " + Math.round(result.total / 1024) + "MB");

                progressCPU.attr("class", "determinate");
                progressCPU.css("width", result.cpu + "%");
                textCPU.text("CPU usage: " + result.cpu + "%");

                progressTPS.attr("class", "determinate");
                progressTPS.css("width", (result.tps * 5) + "%");
                textTPS.text("TPS: " +  Math.round(result.tps));
            },
            error: function (result) {
                Materialize.toast('Connection failed!', 10000,'',function(){connected = true});
                textCPU.text("CPU usage: ?");
                textTPS.text("TPS: ?");
                textRAM.text("RAM: ?");

                connected = false;
            }
        });
    }
}

$(document).ready(function () {
    var socket;
    $.ajax({
        url: "/wsport",
        success: function (result) {
            if (result == parseInt(result, 10)) {
                socket = new WebSocket("ws://" + document.domain + ":" + result + "/");
            } else {
                socket = new WebSocket("wss://" + document.domain + "/" + result + "/");
            }

            socket.onmessage = function (event) {
                term.echo(event.data)
            }
        }});

    var term = $('#term').terminal(function (command, term) {
        if (command !== '') {
            if ((command == 'stop') || (command == 'reload')) {
                var cmd = command;
                term.push(function(command) {
                    if (command.match(/y|yes/i)) {
                        socket.send(cmd);
                        term.pop();
                    } else if (command.match(/n|no/i)) {
                        term.pop();
                    }
                }, {
                    prompt: 'Are you sure? '
                });
            } else {
                socket.send(command);
            }
        }
    }, {
        greetings: '',
        name: 'js_demo',
        height: 400,
        outputLimit: 500,
        prompt: '> '
    });

    if (!jQuery.browser.mobile) {
        $("#cmd_form").hide();
    } else {
        term.disable();
    }

    $("#cmd_box").on('keypress', function (event) {
        if(event.which === 13){
            event.preventDefault()
            socket.send($(this).val());
            $(this).val("");
        }
    });

    $("#cmd_form").submit(function(){
        socket.send( $("#cmd_box").val());
        $("#cmd_box").val("");
        return false;
    });

    window.setInterval(function(){
        refresh();
    }, 1000);

});

