package xyz.dowob.filemanagement.functionInterface;

/**
 * 操作介面，可以將方法作為參數傳遞進行操作
 *
 * @author yuan
 * @program File-Management
 * @ClassName Operation
 * @description
 * @create 2024-09-16 02:39
 * @Version 1.0
 **/
@FunctionalInterface
public interface Operation {
    /**
     * 執行方法，可以將方法作為參數傳遞進行操作
     *
     * @throws Exception 異常
     */
    void execute() throws Exception;
}
