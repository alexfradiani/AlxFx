/* global google */
/* global j_initialdeposit, j_finalequity, j_commissions, jorders, jevents, j_reptitle */

/**
 * Javascript parser of original report
 * to extend statistics of the processed Strategy
 *    1. Chart for equity with google charts
 *    2. metatrader-like values parsed from jforex report log
 * ***UPDATED to work with AlxFX environment
 */

//Variables used for the report
var initialDeposit = 0;
var commissions = 0;

var profitTrades = 0;
var lossTrades = 0;

var grossProfit = 0;
var grossLoss = 0;

var largestProfitTrade = 0;
var largestLossTrade = 0;

var equityData = []; //Array for equity chart

/**
 * Initialize
 */
$(document).ready(function() {
    initReportVals();
    
    //---------------------------------------------------------------------------get initial deposit & commissions from original report
    var th = $("table").find("th:contains('Initial deposit')");
    initialDeposit = Number(th.next().text());
    
    th = $("table").find("th:contains('Commission')");
    commissions = parseFloat(th.next().text().replace(/ /g,''));
    
    //---------------------------------------------------------------------------find table with closed trades
    var table = $("#orders");
    table.find("tr").each(function() {
        //get profit/loss value of that trade
        var pl = $(this).find("td:eq(6)");
        if(pl.length) {  //is not the header row
            pl = Number(pl.text());

            //check if loss or profit trade
            if(pl < 0) {
                lossTrades++;
                grossLoss += pl;

                if(pl < largestLossTrade)
                    largestLossTrade = pl;
            }
            else {
                profitTrades++;
                grossProfit += pl;

                if(pl > largestProfitTrade)
                    largestProfitTrade = pl;
            }

            //column for tooltip
            var label = $(this).find("td:eq(0)").text();
            var amount = $(this).find("td:eq(1)").text();
            var open = $(this).find("td:eq(7)").text();
            var close = $(this).find("td:eq(8)").text();
            var pair = $(this).find("td:eq(2)").text();
            var tooltipData = "<b>"+ pair + "<br/>order: -1</b> (" + label + ") <br/>" +
                    "<b>size:</b> " + amount + "<br/>" + "<b>open</b>: " + open + " <b>close:</b> " + close;

            var item = new Object();
            item.index = -1; //to be defined later at totalization
            item.equityVal = -1;  //same...
            item.tooltipData = tooltipData;
            item.pl = pl;
            item.cdate = close.replace(" ", "T");  //for time format
            item.cdate = new Date(item.cdate);

            equityData.push(item);
        }
    });
    
    prepareTemplate();
    writeXreport();
});

/**
 * Create all the values of the report
 * that are coming from the java source
 */
function initReportVals() {
    $("#report_title").html(j_reptitle);
    $("#initial_deposit").html(j_initialdeposit);
    $("#final_equity").html(j_finalequity.toFixed(2));
    $("#commission").html(j_commissions.toFixed(2));
    
    //fill the orders table
    for(var i = 0; i < jorders.length; i++) {
        var nrow = 
            "<tr>" +
                "<td>" + jorders[i].label + "</td>" +
                "<td>" + jorders[i].size + "</td>" +
                "<td>" + jorders[i].pair + "</td>" +
                "<td>" + jorders[i].type + "</td>" +
                "<td>" + jorders[i].open_price + "</td>" +
                "<td>" + jorders[i].close_price + "</td>" +
                "<td>" + jorders[i].profit.toFixed(2) + "</td>" +
                "<td>" + readableTime(jorders[i].open_time) + "</td>" +
                "<td>" + readableTime(jorders[i].close_time) + "</td>" +
            "</tr>";
        $("#orders tr:last").after(nrow);
    }
    
    //Fill the event log
    for(var i = 0; i < jevents.length; i++) {
        var nrow = 
            "<tr>" +
                "<td>" + jevents[i].when + "</td>" +
                "<td>" + jevents[i].type + "</td>" +
                "<td>" + jevents[i].text + "</td>" +
            "</tr>";
        $("#event_log tr:last").after(nrow);
    }
}

function readableTime(time) {
    //display as GMT
    var offset = new Date().getTimezoneOffset() * 60 * 1000;
    var coef;
    if(offset > 0)
        coef = 1;
    else
        coef = -1;
    var d = new Date(time + coef * offset);
    
    var month = parseInt(d.getMonth() + 1) < 10 ? "0" + parseInt(d.getMonth() + 1) : parseInt(d.getMonth() + 1);
    var day = parseInt(d.getDate()) < 10 ? "0" + d.getDate() : d.getDate();
    var hour = parseInt(d.getHours()) < 10 ? "0" + d.getHours() : d.getHours();
    var minute = parseInt(d.getMinutes()) < 10 ? "0" + d.getMinutes() : d.getMinutes();
    var second = parseInt(d.getSeconds()) < 10 ? "0" + d.getSeconds() : d.getSeconds();
    return d.getFullYear() + "." + month + "." + day + " " + hour + ":" + minute + ":" + second;
}

/**
 * insert HTML template for the report variables
 */
function prepareTemplate() {
    var html = '<br/><div style="width:100%; "> <h2>Performance Chart:</h1> <div style="float:left;"> <div class="label_left">Initial Deposit:</div> <div class="label_right" id="initialDeposit"></div> <div style="clear:both;"></div> <div class="label_left">Total Net Profit:</div> <div class="label_right" id="totalNetProfit"></div> <div style="clear:both;"></div> <div class="label_left">Commissions:</div> <div class="label_right" id="commissions"></div> <div style="clear:both;"></div> <div class="label_left">Final deposit:</div> <div class="label_right" id="finalDeposit"></div> <div style="clear:both;"></div> <div style="margin-top: 20px;"></div> <div class="label_left">Gross Profit:</div> <div class="label_right" id="grossProfit"></div> <div style="clear:both;"></div> <div class="label_left">Gross Loss</div> <div class="label_right" id="grossLoss"></div> <div style="clear:both;"></div> </div> <div style="float:left; margin-left:20px;"> <div class="trades_label_left">Total Trades:</div> <div class="label_right" id="totalTrades"></div> <div style="clear:both;"></div> <div class="trades_label_left">Profit Trades (% of total):</div> <div class="label_right" id="profitTrades"></div> <div style="clear:both;"></div> <div class="trades_label_left bad">Loss Trades (% of total):</div> <div class="label_right" id="lossTrades"></div> <div style="clear:both;"></div> <div class="trades_label_left">Largest Profit Trade:</div> <div class="label_right" id="largestProfitTrade"></div> <div style="clear:both;"></div> <div class="trades_label_left bad">Largest Loss Trade:</div> <div class="label_right" id="largestLossTrade"></div> <div style="clear:both;"></div> <div class="trades_label_left">Average Profit Trade:</div> <div class="label_right" id="averageProfitTrade"></div> <div style="clear:both;"></div> <div class="trades_label_left bad">Average Loss Trade:</div> <div class="label_right" id="averageLossTrade"></div> <div style="clear:both;"></div> </div> <div style="clear:both;"></div> </div> <br/><br/>';
    
    $("#content").append(html);
}

/**
 * write values inside the template
 */
function writeXreport() {
    $("#initialDeposit").html(initialDeposit);
    $("#commissions").html(commissions.toFixed(2));
    $("#grossProfit").html(grossProfit.toFixed(2));
    $("#grossLoss").html(grossLoss.toFixed(2));
    
    var totalNetProfit = grossProfit + grossLoss;
    $("#totalNetProfit").html(totalNetProfit.toFixed(2));
    
    $("#finalDeposit").html((initialDeposit + totalNetProfit - commissions).toFixed(2));
    
    var totalTrades = profitTrades + lossTrades;
    $("#totalTrades").html(totalTrades);
    var pp = profitTrades * 100 / totalTrades;
    $("#profitTrades").html(profitTrades + " (" + pp.toFixed(2) + "%)");
    pp = lossTrades * 100 / totalTrades;;
    $("#lossTrades").html(lossTrades + " (" + pp.toFixed(2) + "%)");
    
    $("#largestLossTrade").html(largestLossTrade.toFixed(2));
    $("#largestProfitTrade").html(largestProfitTrade.toFixed(2));
    
    $("#averageProfitTrade").html((grossProfit / profitTrades).toFixed(2));
    $("#averageLossTrade").html((grossLoss / lossTrades).toFixed(2));
    
    setEquityChart();
}

/**
 * Draw Equity chart using Google Chart API
 */
function setEquityChart() {
    //add div
    $("#content").append("<div id='chart_div' style='padding-right:50px;'></div><br/><br/><br/>");
    
    google.load('visualization', '1', {packages: ['corechart', 'line']});
    google.setOnLoadCallback(g_callback);
}

function g_callback() {
    //data source for the chart
    var data = new google.visualization.DataTable();
    data.addColumn('number', 'X');
    data.addColumn('number', 'Equity');
    data.addColumn({type: 'string', role: 'tooltip', p: {html: true}});
    
    var eqdata = totalizeEquityData();
    data.addRows(eqdata);

    var options = {
      hAxis: {
        title: 'Trades'
      },
      vAxis: {
        title: 'Equity'
      },
      backgroundColor: '#f1f8e9',
      title: 'Equity (without commissions)',
      tooltip: {
          isHtml: true
      }
    };

    var chart = new google.visualization.LineChart(document.getElementById('chart_div'));
    chart.draw(data, options);
}

/**
 * organize all orders based in closed time
 * adjust index property
 * and equity value at the end of each trade
 */
function totalizeEquityData() {
    var data = new Array();
    
    //order equityData array
    equityData.sort(function(a, b) {
        return a.cdate - b.cdate;
    });
    
    //extract only necessary data for google chart and return new array
    for(var i = 0; i < equityData.length; i++) {
        var index = i + 1;
        var pl = equityData[i].pl;
        equityData[i].equityVal = (i === 0) ? initialDeposit + pl : equityData[i - 1].equityVal + pl;
        var eqVal = equityData[i].equityVal;
        
        var tooltipData = equityData[i].tooltipData.replace("order: -1", "order: " + index);
        
        data.push([index, eqVal, tooltipData]);
    }
    
    return data;
}