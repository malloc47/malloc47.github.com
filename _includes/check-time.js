function lightp() {
    var hour = new Date().getHours();
    return (hour < 19 && hour > 6);
}

function check_time() {
    light = lightp();
    if(light && !document.getElementById('day-css')) {
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
    else if(!light && !document.getElementById('night-css')) {
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
    drawClock(light);
}

// check_time();

// Doing this prevents loading the rest of the page before getting the
// appropriate style (which would previously lead to flickering)
if(lightp()) {
    document.write('<link rel="stylesheet" id="day-css" href="/css/day.css">');
}
else {
    document.write('<link rel="stylesheet" id="night-css" href="/css/night.css">');
}
