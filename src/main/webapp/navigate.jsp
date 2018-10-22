<%-- 
    Document   : search
    Created on : 30-Nov-2015, 23:39:51
    Author     : Debasis
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link href="http://fonts.googleapis.com/css?family=Open+Sans&subset=latin,cyrillic" rel="stylesheet"
              type="text/css">
        <link href="css/bootstrap.min.css" rel="stylesheet">
        <link href=defaultstyle.css rel="stylesheet">
        <link href='https://fonts.googleapis.com/css?family=Architects+Daughter' rel='stylesheet' type='text/css'>
        <link rel="stylesheet" href="jstree/dist/themes/default/style.min.css" />
        <link rel='stylesheet' type='text/css' href='datatables/datatables.css'>
        
        <!--link rel="stylesheet" type="text/css" href="css/stylesheet.css" media="screen"/-->
        <!--link rel="stylesheet" type="text/css" href="css/pygment_trac.css" media="screen"/-->
        <!--link rel="stylesheet" type="text/css" href="css/print.css" media="print"/-->
        
        <link href="jquery/themes/ui-lightness/jquery-ui.css" rel="stylesheet">

        <script src="jquery/jquery-2.1.4.js"></script>
        <!--script src="jquery/jquery.js"></script-->
        <script src="jquery/jquery-ui.js"></script>
        <script src="jstree/jquery-1.11.2.min.js"></script>
        <script src="jstree/dist/jstree.min.js"></script>        
        <script type="text/javascript" src="ui.layout/jquery.layout.js"></script>
        <script type='text/javascript' charset='utf8' src='datatables/datatables.js'></script>
        <script src="jquery/spin.js"></script>
        <script src="jquery/jquery.twbsPagination.js" type="text/javascript"></script>        
        
        <script>
            var perFeatureTable = null;
            // spinner
            var opts = {
                lines: 13 // The number of lines to draw
              , length: 28 // The length of each line
              , width: 14 // The line thickness
              , radius: 42 // The radius of the inner circle
              , scale: 1 // Scales overall size of the spinner
              , corners: 1 // Corner roundness (0..1)
              , color: '#000' // #rgb or #rrggbb or array of colors
              , opacity: 0.25 // Opacity of the lines
              , rotate: 0 // The rotation offset
              , direction: 1 // 1: clockwise, -1: counterclockwise
              , speed: 1 // Rounds per second
              , trail: 60 // Afterglow percentage
              , fps: 20 // Frames per second when using setTimeout() as a fallback for CSS
              , zIndex: 2e9 // The z-index (defaults to 2000000000)
              , className: 'spinner' // The CSS class to assign to the spinner
              , top: '50%' // Top position relative to parent
              , left: '50%' // Left position relative to parent
              , shadow: false // Whether to render a shadow
              , hwaccel: false // Whether to use hardware acceleration
              , position: 'absolute' // Element positioning
            };
            var spinner;
    
            function createSpinner(containerName) {
                var target = document.getElementById(containerName);
                spinner = new Spinner(opts).spin(target);
                target.appendChild(spinner.el);                
            }
            
            function viewDoc() {
                var id = this.id;
                var url = "DocumentViewerServlet?id=" + id;
                
                createSpinner("docview");
                
                $.ajax({ url: url,
                        success: function(result) {
                            // set the content of the dialog box
                            // by dynamically loading the content
                            // from the servlet
                            spinner.stop();
                            
                            $("#docview").html(result);
                        }
                });
            }
            
            function retrieveAdhoc(page) {
                if ($("#query").val().length === 0) {
                    $("#query").focus();
                    return;
                }

                var query = $("#query").val().replace(".", " ");
                var url = "PassageSearchServlet?query=" + query + "&page=" + page;
                
                createSpinner("containerdiv");

                $.ajax({ url: url,
                        success: function(result) {
                            spinner.stop();
                            
                            $("#srchres").html(result);

                            // Don't set the click event for the time being.
                            //$(".ResultURLStyle a").button().click(viewDoc);

                            $('#serp').twbsPagination({
                                    totalPages: 10,
                                    visiblePages: 5,
                                    onPageClick: function (event, page) {
                                        retrieveAdhoc(page);
                                    }
                            });   
                            $('#serp').show();                            
                        }});      
            }                       
            
            function addRowHandlers() {

                $('#perfeatureinfo tbody')
                .on('click', 'tr',
                    function () {
                        $("#query").val('');  // clear textbox
                        var data = perFeatureTable.row(this).data();
                        retrieveDocFromTableCell(data[0], data[2]);
                    }
                );
            }
             
            function retrieveDocFromTableCell(attribid, docname) {
                createSpinner("containerdiv");
                
                var url = "PassageSearchServlet?attribid=" + attribid + "&docname=" + docname;                
                $.ajax({
                    url: url,
                    success: function(result) {                        
                        $("#srchres").html(result);
                        spinner.stop();
                    } 
                });
            }             
             
            // Gets information corresponding to selected attribute from the tree 
            function loadExtractedInfo(attribId) {
                
                var url = "ExtractedInfoViewerServlet?attribid=" + attribId;
                
                $.ajax({
                    url: url,
                    success: function(result) {
                        if (perFeatureTable!=null)
                            perFeatureTable.destroy();
                        
                        $("#perfeatureinfo").html(result);
                        perFeatureTable = $('#perfeatureinfo').DataTable({
                            "pageLength": 5,
                            "columnDefs": [
                                { "visible": false, "targets": 0 }
                            ],
                            "columns": [
                                { "width": "1%" },
                                { "width": "9%" },
                                { "width": "20%" },
                                { "width": "15%" },
                                { "width": "30%" },
                                { "width": "20%" },
                                { "width": "5%" }
                            ]
                        });
                        addRowHandlers();
                    } 
                });
            }
            
           
            // Document body loading
            $(document).ready(function() {
                
                myLayout = $('body').layout({
                    applyDemoStyles: true, //	reference only - these options are NOT required because 'true' is the default
                    closable: true,	// pane can open & close
                    resizable: true,	// when open, pane can be resized 
                    slidable: true,	// when closed, pane can 'slide' open over other panes - closes on mouse-out
                    livePaneResizing: true,
                    west__size: '20%',
                    east__size: '25%',
                    //south__size: '20%',
                    //east: {resizable: true, resizeWhileDragging: true, slidable: true},
                    //north: {resizable: true, resizeWhileDragging: true, slidable: true},
                });
               
                // create the exploration tree
                $('#context')
                    .jstree({
                    'core' : {
                        'data' : {
                            "url" : "OntologyTreeServlet?code=0",
                            "dataType" : "json", // needed only if you do not supply JSON headers
                            'data': function(node) {
                                return {'id': node.id};
                            }
                        }
                    }    
                    })
                    .on("changed.jstree",
                        function (e, data) {
                            if(data.selected.length) {
                                loadExtractedInfo(data.instance.get_node(data.selected[0]).id);
                            }
                        }
                    );

                $('#interventions')
                    .jstree({
                    'core' : {
                        'data' : {
                            "url" : "OntologyTreeServlet?code=1",
                            "dataType" : "json", // needed only if you do not supply JSON headers
                            'data': function(node) {
                                return {'id': node.id};
                            }
                        }
                    }    
                    })
                    .on("changed.jstree",
                        function (e, data) {
                            if (data.selected.length) {
                                loadExtractedInfo(data.instance.get_node(data.selected[0]).id);
                            }
                        }
                    );            

                $('#outcomes')
                    .jstree({
                    'core' : {
                        'data' : {
                            "url" : "OntologyTreeServlet?code=2",
                            "dataType" : "json", // needed only if you do not supply JSON headers
                            'data': function(node) {
                                return {'id': node.id};
                            }
                        }
                    }    
                    })
                    .on("changed.jstree",
                        function (e, data) {
                            if (data.selected.length) {
                                loadExtractedInfo(data.instance.get_node(data.selected[0]).id);
                            }
                        }
                    );            
                    
                    $('#serp').hide();
                    //$("#srchBttn").button().click(retrieveFirstPage);
            });

            function retrieveFirstPage() {
                retrieveAdhoc(1);
            }

        </script>
    </head>
    
    <body>        
    <div class="ui-layout-center">        
        <!--table border="0">
        <tbody>
        <tr>
        <td align="center">
        <img src="images/IBM-Watson-Logo.png" alt="IBM Research Dublin"
             border="0" style="max-height:50%;max-width:20%">
        </td>
        <td align="center">
        <img src="images/hbcp.jpg" alt="HBCP"
             border="0" style="max-height:40%;max-width:40%">                            
        </td>
        </tr>
        </tbody>
        </table-->
        <center>
        <img src="images/hbcp.jpg" alt="HBCP"
             border="0" style="max-height:10%;max-width:10%">
        </center>
        <hr>
        
        <div id="tabularinfo" name="tabularinfo">
             <table id="perfeatureinfo" name="perfeatureinfo" class="display compact" cellspacing="0">
             </table>            
        </div>   
    </div>
        
    <div class="ui-layout-west">        
        <div id="context" class="demo" style="font-size: 10px">
        </div>    
        <div id="interventions" class="demo" style="font-size: 10px">
        </div>    
        <div id="outcomes" class="demo" style="font-size: 10px">
        </div>    
    </div>
        
    <div class="ui-layout-east">        
        <div id="containerdiv">
        <center>
        <input type="text" id="query" name="query" size="50">
        <input type="button" id="srchBttn" value="Search" onclick="retrieveAdhoc(1)"/>
        <!--button id="srchBttn">Search</button-->
        </center>
        </div>

        <div id="srchres" name="srchres">
        </div>

        <!-- Pagination component -->
        <div>
            <center>
            <ul id="serp" class="pagination-sm"></ul>
            </center>
        </div>
    </div>
        
    <!--div class="ui-layout-south">        
        <div id="docview" class="demo">
        </div>    
    </div-->
        
    </body>
</html>
