module reg32_avalon(input logic clk, input logic reset_n, input logic [31:0] writedata, 
    output logic [31:0] readdata, input logic write, input logic read, input logic [3:0] byteenable, input logic chipselect, 
    output logic [31:0] Q_export);
    
    wire [3:0] local_byteenable;
    wire [31:0] to_reg, from_reg;
    assign to_reg = writedata;
    assign local_byteenable = (chipselect & write) ? byteenable : 4'd0;

    always_ff @(posedge clk) begin
        if (!reset_n)
            from_reg <= 32'b0;
        else begin
            // Enable writing to each byte separately
            if (local_byteenable[0]) from_reg[7:0] <= to_reg[7:0];
            if (local_byteenable[1]) from_reg[15:8] <= to_reg[15:8];
            if (local_byteenable[2]) from_reg[23:16] <= to_reg[23:16];
            if (local_byteenable[3]) from_reg[31:24] <= to_reg[31:24];
        end
    end
    assign readdata = from_reg;
    assign Q_export = from_reg;
endmodule
