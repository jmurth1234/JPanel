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
                Materialize.toast('Connection failed! Click to retry, or wait 10 seconds.', 10000,'',function(){connected = true});
                textCPU.text("CPU usage: ?");
                textTPS.text("TPS: ?");
                textRAM.text("RAM: ?");

                connected = false;
            }
        });
    }
}

$(document).ready(function () {
    var exampleSocket = new WebSocket("ws://" + document.domain + ":9003/");

    var term = $('#term').terminal(function (command, term) {
        if (command !== '') {
            if ((command == 'stop') || (command == 'reload')) {
                var cmd = command
                term.push(function(command) {
                    if (command.match(/y|yes/i)) {
                        exampleSocket.send(cmd);
                        term.pop();
                    } else if (command.match(/n|no/i)) {
                        term.pop();
                    }
                }, {
                    prompt: 'Are you sure? '
                });
            } else {
                exampleSocket.send(command);
            }
        }
    }, {
        greetings: '',
        name: 'js_demo',
        height: 400,
        outputLimit: 500,
        prompt: '> '
    });

    exampleSocket.onmessage = function (event) {
        console.log(event.data);
        term.echo(event.data)
    }

    window.setInterval(function(){
        refresh();
    }, 1000);

});
