var sc = null
const topic = "akka-ws"

document.addEventListener("DOMContentLoaded", function () {
    // Handler when the DOM is fully loaded

    sc = new WebSocket(`ws://localhost:8000/ws/join/${topic}`)
    sc.onmessage = function (msg) {
        console.log(msg)
    }

    console.log(sc)
});
