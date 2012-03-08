function drawClock() {
    var e = document.getElementById('miniclock');
    if(e.getContext) {
	var c = e.getContext('2d');
	w = e.width;
	h = e.height;
	c.clearRect(0,0,w,h);

	var d = new Date();

	// afternoon
	if(d.getHours()>11) {
	    drawPie(c,w/2,h/2,w/2,hr(12),hr(7),"rgba(0,0,0,0.25)");
	    drawPie(c,w/2,h/2,w/2,hr(7),hr(12),"rgba(0,0,0,0.1)");
	}
	else {
	    // morning
	    drawPie(c,w/2,h/2,w/2,hr(12),hr(6.5),"rgba(0,0,0,0.1)");
	    drawPie(c,w/2,h/2,w/2,hr(6.5),hr(12),"rgba(0,0,0,0.25)");
	}

	// 43200 = (24*60*60)/2
	p = ((d.getHours()*3600+d.getMinutes()*60+d.getSeconds())%43200.0)/43200.0;

	drawLine(c,w/2,h/2,w/3,cp(p),w/66,"rgba(0,0,0,1)");
	drawLine(c,w/2,h/2,w/2.25,cp(d.getMinutes()/60),w/66,"rgba(96,96,96,1)");
	drawLine(c,w/2,h/2,w/2,cp(d.getSeconds()/60),w/100,"rgba(128,128,128,1)");

	// circle to cover up the center
	drawCircle(c,w/2,h/2,w/25,"rgba(0,0,0,0.5)");
	setTimeout("drawClock()",1000);
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

// Convert an hour on the standard clock to an angle
function hr(t) {return (90-t*30 % 360);}
function cp(p) {return (p*360-90);}