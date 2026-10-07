package com.wuyuhang.delivery.service;

import com.wuyuhang.delivery.common.BusinessException;
import com.wuyuhang.delivery.common.enums.ParcelStatus;
import com.wuyuhang.delivery.common.enums.ParcelType;
import com.wuyuhang.delivery.dto.query.ParcelQuery;
import com.wuyuhang.delivery.entity.Parcel;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Excel 台账导出服务。
 * <p>
 * 直接使用 Apache POI 生成 .xlsx 文件，返回字节数组由 Controller 写入响应流。
 * 不依赖任何前端模板，文件格式由后端保证。
 *
 * @author 吴宇航
 */
@Service
public class ExcelExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 台账表头 */
    private static final String[] HEADERS = {
            "序号", "运单号", "快递公司", "快件类型", "收件人", "收件人手机号",
            "取件码", "所在驿站", "库位", "状态", "重量(kg)", "运费(元)",
            "入库时间", "取件时间", "已保管(天)", "逾期费(元)", "入库操作员", "备注"
    };

    /**
     * 把快件列表导出为 Excel 字节数组。
     *
     * @param parcels 待导出的快件数据
     * @param query   导出时使用的查询条件，写入表头下方的说明行
     */
    public byte[] exportParcelLedger(List<Parcel> parcels, ParcelQuery query) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("快件台账");

            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle bodyStyle = buildBodyStyle(workbook);

            // 第一行：导出说明
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("快件收发台账（导出时间："
                    + java.time.LocalDateTime.now().format(DATE_TIME_FORMATTER)
                    + "，共 " + parcels.size() + " 条）");

            // 第二行：表头
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据行
            int rowIndex = 2;
            for (Parcel parcel : parcels) {
                Row row = sheet.createRow(rowIndex);
                int col = 0;
                writeCell(row, col++, String.valueOf(rowIndex - 1), bodyStyle);
                writeCell(row, col++, parcel.getWaybillNo(), bodyStyle);
                writeCell(row, col++, parcel.getExpressCompany(), bodyStyle);
                writeCell(row, col++, ParcelType.labelOf(parcel.getParcelType()), bodyStyle);
                writeCell(row, col++, parcel.getReceiverName(), bodyStyle);
                writeCell(row, col++, parcel.getReceiverPhone(), bodyStyle);
                writeCell(row, col++, parcel.getPickupCode(), bodyStyle);
                writeCell(row, col++, parcel.getStationName(), bodyStyle);
                writeCell(row, col++, parcel.getShelfCode(), bodyStyle);
                writeCell(row, col++, ParcelStatus.labelOf(parcel.getStatus()), bodyStyle);
                writeCell(row, col++, parcel.getWeight() == null ? "" : parcel.getWeight().toPlainString(), bodyStyle);
                writeCell(row, col++, parcel.getFreight() == null ? "" : parcel.getFreight().toPlainString(), bodyStyle);
                writeCell(row, col++, parcel.getInTime() == null ? "" : parcel.getInTime().format(DATE_TIME_FORMATTER), bodyStyle);
                writeCell(row, col++, parcel.getPickupTime() == null ? "" : parcel.getPickupTime().format(DATE_TIME_FORMATTER), bodyStyle);
                writeCell(row, col++, parcel.getStorageDays() == null ? "" : String.valueOf(parcel.getStorageDays()), bodyStyle);
                writeCell(row, col++, parcel.getOverdueFee() == null ? "0.00" : parcel.getOverdueFee().toPlainString(), bodyStyle);
                writeCell(row, col++, parcel.getOperatorName(), bodyStyle);
                writeCell(row, col, parcel.getRemark(), bodyStyle);
                rowIndex++;
            }

            // 自适应列宽（中文按 2 个字符宽度估算）
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.setColumnWidth(i, calculateColumnWidth(HEADERS[i], parcels, i));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("导出台账失败：" + e.getMessage());
        }
    }

    /**
     * 组装导出文件名。
     */
    public String buildFileName() {
        return "快件收发台账_" + java.time.LocalDate.now() + ".xlsx";
    }

    // ==================== 私有方法 ====================

    private void writeCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private CellStyle buildHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorder(style);
        return style;
    }

    private CellStyle buildBodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        style.setWrapText(false);
        applyBorder(style);
        return style;
    }

    private void applyBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    /**
     * 简单估算列宽：中文字符按 2 个宽度单位计算。
     */
    private int calculateColumnWidth(String header, List<Parcel> parcels, int columnIndex) {
        int maxLength = header.length() * 2;
        for (Parcel parcel : parcels) {
            String value = switch (columnIndex) {
                case 1 -> parcel.getWaybillNo();
                case 2 -> parcel.getExpressCompany();
                case 3 -> ParcelType.labelOf(parcel.getParcelType());
                case 4 -> parcel.getReceiverName();
                case 7 -> parcel.getStationName();
                case 9 -> ParcelStatus.labelOf(parcel.getStatus());
                case 17 -> parcel.getRemark();
                default -> null;
            };
            if (value != null) {
                maxLength = Math.max(maxLength, value.length() * 2);
            }
        }
        return Math.min((maxLength + 4) * 256, 60 * 256);
    }
}
