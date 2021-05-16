import serial

test = 1
data = b''

ser = serial.Serial()
ser.baudrate = 115200
ser.port = '/dev/ttyACM0'
ser.timeout = 200


ser.open()
if ( test == 1 ):
	ser.write(b'gon 0\r\n')
	ser.flush()
	ser.flush()
	ser.write(b'sga 10000 0\r\n')
	ser.flush()
	ser.flush()

	ser.write(b'scn22 3336000000 3891000000 500000 200 20 10700000 10000 0\r\n')

#print(ser.in_waiting)
#print(ser.out_waiting)
#ser.flush()
#ser.flush()

# original 9000
#ser.write(b'sga 10000 0\r\n')

#print(ser.in_waiting)
#print(ser.out_waiting)
#ser.flush()
#ser.flush()

# original 9000
#ser.write(b'scn22 3606000000 3891000000 500000 200 20 10700000 10000 0\r\n')

#print(ser.in_waiting)
#print(ser.out_waiting)

line = ser.readline()   # read a '\n' terminated line
print(line)

line1 = ser.readline()   # read a '\n' terminated line
print(line1)

line2 = ser.readline()   # read a '\n' terminated line
print(line2)

line3 = ser.readline()   # read a '\n' terminated line
print(line3)

line4 = ser.readline()   # read a '\n' terminated line
print(line4)

line5 = ser.readline()   # read a '\n' terminated line
print(line5)

while (True):
	line7 = ser.readline()   # read a '\n' terminated line

	print(line7)
	if (line7 ==b'complete\r\n'):
		break
	data+=line7
print('\n\r Final data: \n\r')
print(data)
print('\n\r')
print(len(data))

#line6.decode('iso-8859-1')
index = len(data)-1
while (index>1):
	print(data[index])
	if (data[index] == 255):
		index -=1
		break
	index-=1


print('index:')
print(index)

for i in range(0,index,2):
	if (i > int((index-30)/2)):
  #      print('ind {} val1 {} - val2 {}'.format(i, line6[i], line6[i+1]))
		print(i)
		val_1 = ((data[i] & 0b0111)<<8)
		val_2 = (data[i+1] & 0x0FF)
		print("val_1 ") 
		print(val_1)
    # print("val_2 ")
    # print(val_2)
		val_f = val_1 |val_2
		print("val_f ")
		print((80*10.0-val_f)/10.0)
