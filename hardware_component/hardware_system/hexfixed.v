module hexfixed(input clk, input reset_n,
                  input address,
                  input read, output reg [31:0] readdata,
                  input write, input [31:0] writedata,
                  output reg [6:0] hex0);
	reg [31:0] mem;
	
	always @(*)
      case (writedata) 
         32'd0: hex0 = 7'b1000000;
         32'd1: hex0 = 7'b0001000;
         32'd2: hex0 = 7'b0100100;
         32'd3: hex0 = 7'b0110000;
         32'd4: hex0 = 7'b0011001;
         32'd5: hex0 = 7'b0010010;
         32'd6: hex0 = 7'b0000010;
         32'd7: hex0 = 7'b1111000;
         32'd8: hex0 = 7'b0000000;
         32'd9: hex0 = 7'b0010000;
         32'd10: hex0 = 7'b1000000;
         32'd11: hex0 = 7'b1100001;
         32'd12: hex0 = 7'b0011000;
         32'd13: hex0 = 7'b0001001;
         default: hex0 = 7'b1111111; 
      endcase
	
	always @(posedge clk)
	begin
		if (write)
			mem <= writedata;
		readdata <= mem;
	end
endmodule