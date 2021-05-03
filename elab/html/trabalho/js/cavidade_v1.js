
console.log('Cavidade', env.IP, env.USER, env.HOSTNAME);

//set RPi static IP
var rpiRJ45IP = '192.168.1.81';  //RJ45
var rpiWIFIIP = '192.168.1.82';  //WIFI
var rpiIP = rpiRJ45IP;
var openValvuleTime = 100;

var start_f_txt = "20Mhz"
var stop_f_txt = "30MHz"
var step_f_txt = "0.1MHz"
var n_itera_txt = "5"
var start_f = "3386000000"
var stop_f = "3891000000"
var step_f = "500000"

var file_names = null;




/* var ctx = document.getElementById('myChart');
var myChart = new Chart(ctx, {
    type: 'bar',
    data: {
        labels: ['Red', 'Blue', 'Yellow', 'Green', 'Purple', 'Orange'],
        datasets: [{
            label: '# of Votes',
            data: [14, 19, 3, 5, 2, 3],
            backgroundColor: [
                'rgba(255, 99, 132, 0.2)',
                'rgba(54, 162, 235, 0.2)',
                'rgba(255, 206, 86, 0.2)',
                'rgba(75, 192, 192, 0.2)',
                'rgba(153, 102, 255, 0.2)',
                'rgba(255, 159, 64, 0.2)'
            ],
            borderColor: [
                'rgba(255, 99, 132, 1)',
                'rgba(54, 162, 235, 1)',
                'rgba(255, 206, 86, 1)',
                'rgba(75, 192, 192, 1)',
                'rgba(153, 102, 255, 1)',
                'rgba(255, 159, 64, 1)'
            ],
            borderWidth: 1
        }]
    },
    options: {
        scales: {
            y: {
                beginAtZero: true
            }
        }
    }
});
 */




function myStopFunction() {
  clearInterval(myVar);
}

function myStartFunction() {
  myVar = setInterval(myTimer, 100);
}

function putGPIO() {
    time = $("#time").val();
	var url = 'http://' + rpiIP + ':8085/gpio/switch?pin=4&status=on&time=' + time;
	var url = 'http://' + rpiIP + '/elab/gpio/switch?pin=4&status=on&time=' + time;
    console.log('Button pressed time : ' +  time);
	$.ajax({
      url: url,      //Your api url
      type: 'PUT',   //type is any HTTP method
      data: {
        data: time
      },      //Data as js object
      success: function (response) {
		console.log('PUT Response Pin : ' +  response.pin);
		console.log('PUT Response Pin : ' +  response.result);
      }
    });
}

function getPressure() {
	var url = 'http://' + rpiIP + '/elab/pressure';
	$.ajax({
      url: url,      //Your api url
      type: 'GET',   //type is any HTTP method
      data: {
        data: null
      },      //Data as js object
      success: function (response) {
		console.log('GET Response Result : ' +  response.result);
		document.getElementById('pressure').innerHTML = 'Pressure [mbar]: ' + response.pressure;
      }
    });
}

function getCsv() {
	var url = 'http://' + rpiIP + '/comm/arinst/list';
	$.ajax({
      url: url,      //Your api url
      type: 'GET',   //type is any HTTP method
      data: {
        data: null
      },      //Data as js object
      success: function (response) {
		console.log('GET Response Result : ' +  response);
		document.getElementById('filenames').innerHTML = 'Files : ' + response;
		printBtn(response)
		file_names = response;
      }
    });
}




function putArinst() {
	//if (!$("#start_f").val() == null){
		start_f = $("#start_f").val();
	//}
	//if (!$("#stop_f").val() == null){
		stop_f = $("#stop_f").val();
	//}
	//if (!$("#step_f").val() == null){
			step_f = $("#step_f").val();
    //}
	///elab/arinst?port=COM9&start=3386000000&stop=3891000000&step=500000
	var url = 'http://' + rpiIP + '/comm/arinst?start=' + start_f + '&stop=' + stop_f + '&step=' + step_f;
    console.log('Button pressed start_f cavidade : ' + url);
	$.ajax({
      url: url,      //Your api url
      type: 'GET',   //type is any HTTP method
      data: {
        data: null
      },      //Data as js object
      success: function (response) {
        console.log('PUT Response Pin : ' +  response.frequency);
		desenharCSV(response);
      }
    });
}



 
                //the array
function printBtn(listBrand) {
    for (var i = 0; i < listBrand.length; i++) {
        var btn = document.createElement("button");
        btn.setAttribute("type", "file");
        btn.setAttribute("id", listBrand[i]); 
        var url = 'http://' + rpiIP + '/comm/arinst/csv/'+ listBrand[i];
        btn.setAttribute("onclick", 'getFileCSV(\''+listBrand[i]+'\')');
        var t = document.createTextNode(listBrand[i]);
        btn.appendChild(t);
        /* btn.onclick = function(){
        window.location.href = url;
        return false;
        }; */
        document.body.appendChild(btn);
		}
}

function getFileCSV(fileName){
    var url = 'http://' + rpiIP + '/comm/arinst/csv/'+ fileName;
    console.log('Button pressed start_f cavidade : ' + url);
    window.location.href = url;
/* 	$.ajax({
      url: url,      //Your api url
      type: 'GET',   //type is any HTTP method
      data: {
        data: null
      },      //Data as js object
      success: function (response) {
        console.log('PUT Response Pin : ' +  response);
        
      }
    }); */
}


var amps;

let fre = [];
let amp = [];
let amp2 = [];


function parseData1() {
	var uploa = '/home/pi/Cavidade/elab/webcomm/uploads/';
	var res = uploa.concat(file_names[0]);
	let data_f =[];
	let helper = null;
	Papa.parse(file_names[0], {
		
		download: true,
		complete: function(results) {
			//console.log(results.data);
			for (let i=1; i < results.data.length-1; i++){
				
				//console.log(results.data[i][1]);
				helper =results.data[i][1].replace(/,/g, '.');
				//console.log(helper);
				
				//fre.push(parseFloat(results.data[i][0]));
					//console.log(results.data[i][1]);
				amp.push(helper);
					//console.log(amp);
								
				data_f.push(results.data[i]);
			}
			
		}
		
		
		
	});
			
	
}


function parseData2() {
	var uploa = '/home/pi/Cavidade/elab/webcomm/uploads/';
	var res = uploa.concat(file_names[0]);
	let data_f =[];
	let helper = null;
	Papa.parse(file_names[1], {
		
		download: true,
		complete: function(results) {
			//console.log(results.data);
			for (let i=1; i < results.data.length-1; i++){
				
				//console.log(results.data[i][1]);
				helper =results.data[i][1].replace(/,/g, '.');
				//console.log(helper);
				
				//fre.push(parseFloat(results.data[i][0]));
					//console.log(results.data[i][1]);
				amp2.push(helper);
					//console.log(amp);
								
				data_f.push(results.data[i]);
			}
			
		}
		
		
		
	});
			
	
}


			
			
			//console.log(fre);
function drawGraph() {	
			console.log(amp2);
			var ctx = document.getElementById('graph_1');
			var myChart = new Chart(ctx, {
				type: 'line',
				data: {
					labels: fre,
					datasets: [{
						label: 'Data 1',
						data: amp,
						backgroundColor:'rgb(0,200,0)',
						borderColor:'rgb(0,200,0)',
					},{
						label: 'Data 2',
						data: amp2,
						backgroundColor:'rgb(0,100,0)',
						borderColor:'rgb(0,100,0)',
					}]
				},
				options: {
					scales: {
						y: {
							suggestedMin: -120,
							suggestedMax: -90
						}
					}
				}
			});
			
		
}




function desenharCSV(results) {
			for (let i=1; i < results.length-1; i++){
				
				//console.log(results.data[i][1]);
				helper =results[i].amplitude;//.replace(/,/g, '.');
				//console.log(helper);
				
				fre.push(results[i].frequency);
					//console.log(results.data[i][1]);
				amp.push(helper);
					//console.log(amp);
			}	
	
			console.log(amp2);
			var ctx = document.getElementById('graph_1');
			var myChart = new Chart(ctx, {
				type: 'line',
				data: {
					labels: fre,
					datasets: [{
						label: 'Data 1',
						data: amp,
						backgroundColor:'rgb(0,200,0)',
						borderColor:'rgb(0,200,0)',
					}]
				},
				options: {
					scales: {
						y: {
							suggestedMin: -120,
							suggestedMax: 0
						}
					},
                    elements:{
                        point:{
                            radius: 1
                        },
                        line:{
                            borderWidth: 1
                        }
                        
                    }
				}
			});
	
}


			
			









/* function parseData() {
	var uploa = '/home/pi/Cavidade/elab/webcomm/uploads/';
	var res = uploa.concat(file_names[0]);
	let fre = [];
	let amp = [];
	let data_f =[];
	let helper = null;
	Papa.parse(file_names[0], {
		download: true,
		complete: function(results) {
			console.log(results.data);
			for (let i=1; i < results.data.length-1; i++){
				fre.push(parseFloat(results.data[i][0]));
				//console.log(results.data[i][1]);
				helper =results.data[i][1].replace(/,/g, '.');
				//console.log(helper);
				amp.push(helper);
				
				data_f.push(results.data[i]);
			}
			
			
			//console.log(fre);
			//console.log(amp);	
			
			var ctx = document.getElementById('graph_1');
			var myChart = new Chart(ctx, {
				type: 'line',
				data: {
					labels: fre,
					datasets: [{
						label: 'Data 1',
						data: amp,
						backgroundColor:'rgb(0,200,0)',
						borderColor:'rgb(0,200,0)',
					}]
				},
				options: {
					scales: {
						y: {
							suggestedMin: -120,
							suggestedMax: -90
						}
					}
				}
			});
			
		}
	});
} */



/* var ctx = document.getElementById('graph');
var myChart = new Chart(ctx, {
	type: 'line',
	data: {
	  labels: [65, 59, 80, 81, 56, 55, 40],
	  datasets: [{
		label: 'My First Dataset',
		data: [65, 59, 80, 81, 56, 55, 40],
		fill: false,
		borderColor: 'rgb(75, 192, 192)',
	  }]
	}
});
 */



$(document).ready(function(){
  //$("start_f").val(start_f_txt);
  //$("stop_f").val(stop_f_txt);
  //$("step_f").val(step_f_txt);
  //$("n_itera").val(n_itera_txt);

	function startReadPresssure(times) {
	  let numberSelected = 0;
	  for (let i = 0; i < times; i++) {
		console.log('Number selected' + numberSelected);
		numberSelected++;
		document.getElementById('pressure').innerHTML = 'Pressure : ' + numberSelected;
	  }
	}

	function stopReadPresssure() {
	  clearInterval(myVar);
	}

  
});
