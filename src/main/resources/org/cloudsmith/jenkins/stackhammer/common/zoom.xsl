<?xml version="1.0" encoding="UTF-8"?>
<!--
 Copyright 2012-, Cloudsmith Inc.

 Licensed under the Apache License, Version 2.0 (the "License"). You may not
 use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 License for the specific language governing permissions and limitations
 under the License.
-->
<xsl:transform
	version="1.0"
	xmlns="http://www.w3.org/2000/svg"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:svg="http://www.w3.org/2000/svg"
	xmlns:xlink="http://www.w3.org/1999/xlink"
	exclude-result-prefixes="svg xlink">
	<xsl:output method="xml" indent="yes"/>
		<xsl:param name="stackhammer"/>
	<xsl:template match="/svg:svg">
		<xsl:element name="svg">
			<xsl:attribute name="onload">init(evt, '<xsl:value-of select="@width"/>', '<xsl:value-of select="@height"/>')</xsl:attribute>
			<xsl:copy-of select="@*" />
			<style>
		  		.territory:hover{
					fill:           #22aa44;
				}
		  		.compass{
		  			fill:			#fff;
		  			stroke:			#000;
		  			stroke-width:	1.5;
		  		}
		   		.button{
				    fill:           	#225EA8;
					stroke:   			#0C2C84;
					stroke-miterlimit:	6;
					stroke-linecap:		round;
				}
				.button:hover{
					stroke-width:   	2;
				}
				.plus-minus{
					fill:	#fff;
					pointer-events: none;
				}
			</style>
			<script>
				<xsl:attribute name="xlink:href"><xsl:value-of select="$stackhammer"/>/zoom.js</xsl:attribute>
			</script>

			<xsl:copy-of select="node()" />

			<circle cx="50" cy="80" r="42" fill="white" opacity="0.75"/>
			<path class="button" onclick="pan( 0, 50)" d="M50 40 l12   20 a40, 70 0 0,0 -24,  0z" />
			<path class="button" onclick="pan( 50, 0)" d="M10 80 l20  -12 a70, 40 0 0,0   0, 24z" />
			<path class="button" onclick="pan( 0,-50)" d="M50 120 l12  -20 a40, 70 0 0,1 -24,  0z" />
			<path class="button" onclick="pan(-50, 0)" d="M90 80 l-20 -12 a70, 40 0 0,1   0, 24z" />
			  
			<circle class="compass" cx="50" cy="80" r="20"/>
			<circle class="button"  cx="50" cy="71" r="8" onclick="zoom(0.8)"/>
			<circle class="button"  cx="50" cy="89" r="8" onclick="zoom(1.25)"/>
			
			<rect class="plus-minus" x="46" y="69.5" width="8" height="3"/>
			<rect class="plus-minus" x="46" y="87.5" width="8" height="3"/>
			<rect class="plus-minus" x="48.5" y="85" width="3" height="8"/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="node()|@*">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>
</xsl:transform>

