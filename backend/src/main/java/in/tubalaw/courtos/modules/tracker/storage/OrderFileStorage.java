package in.tubalaw.courtos.modules.tracker.storage;

public interface OrderFileStorage {
    /** Stores bytes, returns a storage key/path you'll persist on CaseOrder */
    String store(String tenantId, String cnr, String filename, byte[] bytes) throws Exception;

    /** Retrieves bytes given the stored key */
    byte[] retrieve(String storageKey) throws Exception;

    boolean exists(String storageKey);
}
