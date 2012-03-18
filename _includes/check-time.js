function lightp() {
    var hour = new Date().getHours();
    return (hour < 19 && hour > 6);
}

function check_time() {
    if(!window.theme_override) {
	window.theme = lightp();
    }
    if(window.theme && !document.getElementById('day-css')) {
	var dLink = document.createElement('link')
	dLink.href = '/css/day.css';
	dLink.rel = 'stylesheet';
	dLink.type = 'text/css';
	dLink.id = 'day-css';
	document.head.appendChild(dLink);
	dLink = null;
	// remove old header to prevent link tags building up if some
	// poor soul leaves this site open for too long
	if(document.getElementById('night-css')) {
	    document.head.removeChild(document.getElementById('night-css'));
	}
    }
    else if(!window.theme && !document.getElementById('night-css')) {
	var dLink = document.createElement('link')
	dLink.href = '/css/night.css';
	dLink.rel = 'stylesheet';
	dLink.type = 'text/css';
	dLink.id = 'night-css';
	document.head.appendChild(dLink);
	dLink = null;
	if(document.getElementById('day-css')) {
	    document.head.removeChild(document.getElementById('day-css'));
	}
    }
    drawClock();
}

function clock_button() {
    window.theme_override = false;
    window.theme = lightp();
    create_cookie('theme_override', window.theme_override, 7);
    create_cookie('theme', window.theme, 7);
    check_time();
}

function toggle_button() {
    window.theme_override = true;
    window.theme = !window.theme;
    create_cookie('theme_override', window.theme_override, 7);
    create_cookie('theme', window.theme, 7);
    check_time();
}

function day_button() {
    // reset override if its already active
    window.theme_override = !(window.theme && window.theme_override);
    window.theme = true;
    create_cookie('theme_override', window.theme_override, 7);
    create_cookie('theme', window.theme, 7);
    check_time();
}

function night_button() {
    window.theme_override = !(!window.theme && window.theme_override);
    window.theme = false;
    create_cookie('theme_override', window.theme_override, 7);
    create_cookie('theme', window.theme, 7);
    check_time();
}

var cookie1 = read_cookie('theme_override')==null ? false : read_cookie('theme_override') == "true";
var cookie2 = read_cookie('theme')==null ? false : read_cookie('theme') == "true";

window.theme_override = cookie1;

window.theme = window.theme_override ? cookie2 : lightp()

// Doing this prevents loading the rest of the page before getting the
// appropriate style (which would previously lead to flickering)
if(window.theme) {
    document.write('<link rel="stylesheet" id="day-css" href="/css/day.css">');
}
else {
    document.write('<link rel="stylesheet" id="night-css" href="/css/night.css">');
}