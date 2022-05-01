function lightp() {
    if (window.matchMedia) {
	return !window.matchMedia('(prefers-color-scheme: dark)').matches
    }
    else {
	var hour = new Date().getHours();
	return (hour < 19 && hour > 6);
    }
}

function check_time() {
    if(!window.theme_override) {
	window.theme = lightp();
    }
    document.getElementById('day-css').disabled = window.theme;
    document.getElementById('night-css').disabled = !window.theme;
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

if (window.matchMedia) {
    window.matchMedia('(prefers-color-scheme: dark)')
	.addEventListener('change', function(event) {
	    check_time();
	});
}

check_time();
