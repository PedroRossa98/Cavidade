
console.log('Cavidade', env.IP, env.USER, env.HOSTNAME);

// set RPi static IP
var rpiRJ45IP = '192.168.1.81';  // RJ45
var rpiWIFIIP = '192.168.1.82';  // WIFI
var rpiIP = rpiRJ45IP;
var openValvuleTime = 100;

var time = "Inserir o tempo (s)"

let myVar = setInterval(myTimer, 1000);


// var ctx = document.getElementById('myChart');
// var myChart = new Chart(ctx, {
//     type: 'bar',
//     data: {
//         labels: ['Red', 'Blue', 'Yellow', 'Green', 'Purple', 'Orange'],
//         datasets: [{
//             label: '# of Votes',
//             data: [14, 19, 3, 5, 2, 3],
//             backgroundColor: [
//                 'rgba(255, 99, 132, 0.2)',
//                 'rgba(54, 162, 235, 0.2)',
//                 'rgba(255, 206, 86, 0.2)',
//                 'rgba(75, 192, 192, 0.2)',
//                 'rgba(153, 102, 255, 0.2)',
//                 'rgba(255, 159, 64, 0.2)'
//             ],
//             borderColor: [
//                 'rgba(255, 99, 132, 1)',
//                 'rgba(54, 162, 235, 1)',
//                 'rgba(255, 206, 86, 1)',
//                 'rgba(75, 192, 192, 1)',
//                 'rgba(153, 102, 255, 1)',
//                 'rgba(255, 159, 64, 1)'
//             ],
//             borderWidth: 1
//         }]
//     },
//     options: {
//         scales: {
//             y: {
//                 beginAtZero: true
//             }
//         }
//     }
// });
function rand() {
  return Math.random();
}

Plotly.plot('graph', [{
  y: [1].map(rand),
  mode: 'lines+markers',
  marker: {color: 'pink', size: 8},
  line: {width: 4}
}]);

var cnt = 0;
var interval = setInterval(function() {
  Plotly.extendTraces('graph', {
    y: [[rand()]]
  }, [0])

  cnt = cnt+1;
  if(cnt > 100) {
    Plotly.relayout('graph',{
        xaxis: {
            range: [cnt-100,cnt]
        }
    });
    }
}, 100);

function myTimer() {
  var d = new Date();
  var t = d.toLocaleTimeString();
  document.getElementById("demo").innerHTML = t;
  console.log('Button : ', document.getElementById('togBtn').checked);
  getPressure();
}

function myStopFunction() {
  clearInterval(myVar);
}

function myStartFunction() {
  myVar = setInterval(myTimer, 100);
}

function putGPIO() {
    time = $("#time").val();
	// var url = 'http://' + rpiIP + ':8085/gpio/switch?pin=4&status=on&time=' + time;
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




$(document).ready(function(){
  $("input:text").val(time);

	function startReadPresssure(times) {
	  let numberSelected = 0;
	  for (let i = 0; i < times; i++) {
		console.log('Number selected' + numberSelected);
		numberSelected++;
		// document.getElementById('pressure').innerHTML = 'Pressure : ' + numberSelected;
	  }
	}

	function stopReadPresssure() {
	  clearInterval(myVar);
	}

  $("button").click(function(){
	putGPIO();
  });
});
