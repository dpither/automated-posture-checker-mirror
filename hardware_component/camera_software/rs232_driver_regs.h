/**
 * Refer to the RS232.pdf IP core documentation for details
*/

#ifndef RS232_DRIVER_REGS_H
#define RS232_DRIVER_REGS_H

//#define IOWR_RS232_ADDR(base, addr, data)  \
//		IOWR(base, addr, data)
/*
 * Data Register 
 */
#define RS232_DATA_REG			   0

#define IOADDR_RS232_DATA(base)	  \
		((void *)(((uint32_t*)base) + (RS232_CONTROL_REG)))
//#define IORD_RS232_DATA(base)		\
//		IORD(base, RS232_DATA_REG)
//#define IOWR_RS232_DATA(base, data)  \
//		IOWR(base, RS232_DATA_REG, data)

#define RS232_DATA_DATA_MSK			(0x000001FF)
#define RS232_DATA_DATA_OFST			(0)
#define RS232_DATA_PE_MSK			(0x00000200)
#define RS232_DATA_PE_OFST			(9)
#define RS232_DATA_RVALID_MSK		(0x00008000)
#define RS232_DATA_RVALID_OFST		(15)
#define RS232_DATA_RAVAIL_MSK		(0xFFFF0000)
#define RS232_DATA_RAVAIL_OFST		(16)

#define RS232_DATA_REG_RAVAIL		2
//#define IORD_RS232_RAVAIL(base)		\
//		IORD_16DIRECT(base, RS232_DATA_REG_RAVAIL)
#define RS232_RAVAIL_MSK				(0x0000FFFF)
#define RS232_RAVAIL_OFST			(0)

#define RS232_DATA_VALID_MSK			(RS232_DATA_DATA_MSK 	\
											| RS232_DATA_PE_MSK 	\
											| RS232_DATA_RAVAIL_MSK)
/*
 * Control Register 
 */
#define RS232_CONTROL_REG				1

#define IOADDR_RS232_CONTROL(base)		\
		((void *)(((uint32_t*)base) + (RS232_CONTROL_REG)))
//#define IORD_RS232_CONTROL(base)			\
//		IORD(base, RS232_CONTROL_REG)
//#define IOWR_RS232_CONTROL(base, data)	\
//		IOWR(base, RS232_CONTROL_REG, data)

#define RS232_CONTROL_RE_MSK				(0x00000001)
#define RS232_CONTROL_RE_OFST			(0)
#define RS232_CONTROL_WE_MSK				(0x00000002)
#define RS232_CONTROL_WE_OFST			(1)
#define RS232_CONTROL_RI_MSK				(0x00000100)
#define RS232_CONTROL_RI_OFST			(8)
#define RS232_CONTROL_WI_MSK				(0x00000200)
#define RS232_CONTROL_WI_OFST			(9)
#define RS232_CONTROL_WSPACE_MSK			(0xFFFF0000)
#define RS232_CONTROL_WSPACE_OFST		(16)

#define RS232_CONTROL_VALID_MSK			(RS232_CONTROL_RE_MSK 	\
												| RS232_CONTROL_WE_MSK 	\
												| RS232_CONTROL_RI_MSK 	\
												| RS232_CONTROL_WI_MSK 	\
												| RS232_CONTROL_WSPACE_MSK)
#endif	
