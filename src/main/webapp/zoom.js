var transMatrix = [1,0,0,1,0,0];
var width = 0;
var height = 0;
var mapMatrix = null;
var svg;

function init(evt, graphWidth, graphHeight)
{
	svg = evt.target;

	var svgDoc = window.svgDocument;
	if ( svgDoc == null ) {
		svgDoc = svg.ownerDocument;
	}
	

	mapMatrix = svgDoc.getElementById("graph1");
	width  = Number(graphWidth.substring(0, graphWidth.length - 2));
	height = Number(graphHeight.substring(0, graphHeight.length - 2));
	transMatrix[5] = height;
	var newMatrix = "matrix(" +  transMatrix.join(' ') + ")";
	mapMatrix.setAttributeNS(null, "transform", newMatrix);
}

function pan(dx, dy)
{
	transMatrix[4] += dx;
	transMatrix[5] += dy;

	var newMatrix = "matrix(" +  transMatrix.join(' ') + ")";
	mapMatrix.setAttributeNS(null, "transform", newMatrix);
}

function zoom(scale) {
	for (var i=0; i < transMatrix.length; i++) {
		transMatrix[i] *= scale;
	}
	transMatrix[4] += (1-scale)*width/2;
	transMatrix[5] += (1-scale)*height/2;

	var newMatrix = "matrix(" +  transMatrix.join(' ') + ")";
	mapMatrix.setAttributeNS(null, "transform", newMatrix);

	var currentScale = transMatrix[0];
	svg.setAttributeNS(null, "viewBox", "0.00 0.00 " + currentScale * width + " " + currentScale * height);
	svg.setAttributeNS(null, "width", "" + currentScale * width + "pt");
	svg.setAttributeNS(null, "height", "" + currentScale * height + "pt");
}
