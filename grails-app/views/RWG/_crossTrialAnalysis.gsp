
<script type="text/javascript">		
	jQuery(function ($) {

		displayxtAnalysesList();

    	jQuery( "#xtSelectedAnalysesList" ).sortable();
    	jQuery( "#xtSelectedAnalysesList" ).disableSelection();

    	getTopGenes('1085985068');

		
	});
</script>



<div id="xtHolder">
	<div id="xtMenuBar">
		<h1>Cross Trial Analysis</h1>
		<ul id="xtMenu">
			<li>Summary</li>
			<li>Heatmap</li>
			<li>Boxplot</li>
		</ul>
	</div>
	<div id="xtSummary"><!-- Summary Tab Content -->
			
	<h2>Selected Analyses</h2>
	
	<div id="xtSummary_AnalysesList"></div>
	
	<div id="xtTopGenes"></div>
			

</div>
<div id="xtHeatmap"><!-- Heatmap Tab Content -->


</div>
<div id="xtBoxplot"><!-- Boxplot Tab Content -->
	
	
	</div>
</div>

