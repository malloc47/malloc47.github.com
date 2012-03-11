function check_time() {
    var hour = new Date().getHours();
    light = (hour < 19 && hour > 6);
    if(light && !document.getElementById('day-css')) {
	if(document.getElementById('night-css')) {
	    document.head.removeChild(document.getElementById('night-css'));
	}
	var dLink = document.createElement('link')
	dLink.href = '/css/day.css';
	dLink.rel = 'stylesheet';
	dLink.type = 'text/css';
	dLink.id = 'day-css';
	document.head.appendChild(dLink);
	dLink = null;
    }
    else if(!light && !document.getElementById('night-css')) {
	if(document.getElementById('day-css')) {
	    document.head.removeChild(document.getElementById('day-css'));
	}
	var dLink = document.createElement('link')
	dLink.href = '/css/night.css';
	dLink.rel = 'stylesheet';
	dLink.type = 'text/css';
	dLink.id = 'night-css';
	document.head.appendChild(dLink);
	dLink = null;
    }
    drawClock(light);
}

// Doing this prevents loading the rest of the page before calling the
// above function
if((new Date().getHours() < 19 && new Date().getHours() > 6)) {
    document.write('<link rel="stylesheet" id="day-css" href="/css/day.css">');
}
else {
    document.write('<link rel="stylesheet" id="night-css" href="/css/night.css">');
}
