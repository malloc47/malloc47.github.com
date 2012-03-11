function drawClock(cselect) {
    if(cselect) {
	var colors = light_colors();
    }
    else {
	var colors = dark_colors();
    }

    var d = new Date();

    if((d.getHours() < 19 && d.getHours() > 6) && !cselect) {
	check_time();
	return;
    }
    else if(!(d.getHours() < 19 && d.getHours() > 6) && cselect) {
	check_time();
	return;
    }

    var e = document.getElementById('miniclock');
    if(e.getContext) {
	var c = e.getContext('2d');
	w = e.width;
	h = e.height;
	c.clearRect(0,0,w,h);

	// afternoon
	if(d.getHours()>11) {
	    drawPie(c,w/2,h/2,w/2,hr(12),hr(7),colors.dark);
	    drawPie(c,w/2,h/2,w/2,hr(7),hr(12),colors.light);
	}
	else {
	    // morning
	    drawPie(c,w/2,h/2,w/2,hr(12),hr(6.5),colors.light);
	    drawPie(c,w/2,h/2,w/2,hr(6.5),hr(12),colors.dark);
	}

	// 43200 = (24*60*60)/2
	p = ((d.getHours()*3600+d.getMinutes()*60+d.getSeconds())%43200.0)/43200.0;

	drawLine(c,w/2,h/2,w/3,cp(p),w/66,colors.hour);
	drawLine(c,w/2,h/2,w/2.25,cp(d.getMinutes()/60),w/66,colors.minute);
	drawLine(c,w/2,h/2,w/2,cp(d.getSeconds()/60),w/100,colors.second);

	// circle to cover up the center
	drawCircle(c,w/2,h/2,w/25,colors.center);
	setTimeout("drawClock("+cselect+")",1000);
    }
}

function drawCircle(c,p1,p2,r,clr) {
    c.save();
    c.fillStyle = clr;
    c.beginPath();
    c.arc(p1, p2, r, 0, Math.PI*2, false); 
    c.closePath();
    c.fill();
    c.restore();
}

function drawPie(c,p1,p2,r,a1,a2,clr) {
    // convert into standard unit circle coords
    var r1 = (Math.PI/180)*(360-a1);
    var r2 = (Math.PI/180)*(360-a2);
    c.save();
    c.fillStyle = clr;
    c.beginPath();
    c.arc(p1, p2, r, r2, r1, false); 
    c.lineTo(p1,p2);
    c.closePath();
    c.fill();
    c.restore();
}

// Yes, the angle here is backwards from the unit circle angle in the
// above functions (because this is for drawing time)
function drawLine(c,p1x,p1y,d,angle,w,clr) {
    var r = (Math.PI/180)*angle;
    c.save();
    c.strokeStyle = clr;
    c.lineWidth = w;
    c.beginPath();
    c.moveTo(p1x,p1y);
    c.lineTo(p1x+d*Math.cos(r),p1y+d*Math.sin(r));
    c.stroke();
    c.restore();
}

function light_colors() {
    var colors = {};
    colors.dark = "rgba(0,0,0,0.25)";
    colors.light = "rgba(0,0,0,0.1)";
    colors.hour = "rgba(0,0,0,1)";
    colors.minute = "rgba(96,96,96,1)";
    colors.second = "rgba(128,128,128,1)";
    colors.center = "rgba(0,0,0,0.5)";
    return colors;
}

function dark_colors() {
    var colors = {};
    colors.dark = "rgba(255,255,255,0.1)";
    colors.light = "rgba(255,255,255,0.25)";
    colors.hour = "rgba(255,255,255,1)";
    colors.minute = "rgba(159,159,159,1)";
    colors.second = "rgba(128,128,128,1)";
    colors.center = "rgba(255,255,255,0.5)";
    return colors;
}

// Convert an hour on the standard clock to an angle
function hr(t) {return (90-t*30 % 360);}
function cp(p) {return (p*360-90);}