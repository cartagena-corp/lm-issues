package com.cartagenacorp.lm_issues.util;

public class ConstantUtil {

    private ConstantUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static final String PERMISSION_DENIED = "No tiene permisos para realizar esta acción";
    public static final String DATA_INTEGRITY_FAIL_MESSAGE = "Problemas con la integridad de los datos";
    public static final String RESOURCE_NOT_FOUND = "Recurso no encontrado";
    public static final String INVALID_INPUT = "Entrada inválida";
    public static final String INVALID_UUID = "El ID proporcionado no es un UUID válido";
    public static final String INTERNAL_SERVER_ERROR = "Error interno del servidor";
    public static final String ORGANIZATION_NAME_ALREADY_EXISTS = "El nombre de la organización ya está en uso";
    public static final String ACCESS_EXCEPTION = "El servicio externo no está disponible o no se pudo acceder a él";
    public static final String ROLE_INITIALIZATION_ERROR = "Error al inicializar roles por defecto";
    public static final String ROLE_DELETE_ERROR = "Error al eliminar roles de la organización";
    public static final String CONFIG_INITIALIZATION_ERROR = "Error al inicializar configuraciones por defecto";
    public static final String CONFIG_DELETE_ERROR = "Error al eliminar configuraciones de la organización";

    public class Success {

        public static final String RESOURCE_DELETED_SUCCESSFULLY = "Issue eliminado correctamente";
        public static final String RESOURCES_DELETED_SUCCESSFULLY = "Issues eliminados correctamente";

        public Success() {
            throw new IllegalStateException("Util class");
        }

    }

}
