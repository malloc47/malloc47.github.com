function check_time() {
    var hour = new Date().getHours();
    light = (hour < 19 && hour > 6);
    if(light) {
	var dLink = document.createElement('link')
	dLink.href = 'css/day.css';
	dLink.rel = 'stylesheet';
	dLink.type = 'text/css';
	document.body.appendChild(dLink);
	dLink = null;
    }
    else {
	var dLink = document.createElement('link')
	dLink.href = 'css/night.css';
	dLink.rel = 'stylesheet';
	dLink.type = 'text/css';
	document.body.appendChild(dLink);
	dLink = null;
    }
    drawClock(light);
}