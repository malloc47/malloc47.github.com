window.addEventListener("load", function(){
    document.getElementById("toggle").onclick = function() {
	toggle_button();
    }
    document.getElementById("miniclock").onclick = function() {
    	clock_button();
    }
    check_time();
    drawClock();
});
