#language: es
@RegistroProveedor @HU02
Característica: Registro de proveedor de servicios
  Como dueño de un negocio (clínica, salón de belleza, consultorio)
  Quiero registrarme como proveedor de servicios
  Para poder publicar mis servicios y gestionar mis reservas

  Antecedentes:
    Dado que el sistema está operativo
    Y existe la categoría "Belleza y Spa" activa

  @CP-02-001 @HappyPath
  Escenario: Registro exitoso de un proveedor
    Dado que el proveedor no tiene cuenta en la plataforma
    Cuando ingresa el nombre del negocio "Salón Bella Vida"
    Y ingresa su correo "salon@bellavida.com"
    Y ingresa una contraseña válida "Segura#123"
    Y selecciona la categoría de servicio "Belleza y Spa"
    Y ingresa su número de contacto "3001234567"
    Y ingresa su dirección "Calle 10 #20-30"
    Y selecciona el rol "Proveedor"
    Y hace clic en "Registrarse"
    Entonces el sistema crea la cuenta del proveedor
    Y el tipo de usuario es "PROVEEDOR"
    Y envía un correo de verificación a "salon@bellavida.com"
    Y muestra el mensaje "Cuenta creada. Por favor verifica tu correo"

  @CP-02-002 @Error @CategoriaInvalida
  Escenario: Registro fallido por categoría no seleccionada
    Dado que el proveedor está completando el formulario de registro
    Cuando no selecciona ninguna categoría de servicio
    Y completa los demás campos correctamente
    Y hace clic en "Registrarse"
    Entonces el sistema muestra el error "Debes seleccionar una categoría de servicio"
    Y no crea la cuenta del proveedor

  @CP-02-003 @Error @CategoriaNoExiste
  Escenario: Registro fallido por categoría inexistente
    Dado que el proveedor está completando el formulario de registro
    Cuando selecciona una categoría inexistente "CategoríaFalsa123"
    Y completa los demás campos correctamente
    Y hace clic en "Registrarse"
    Entonces el sistema muestra el error "La categoría seleccionada no existe o no está activa"
    Y no crea la cuenta del proveedor

  @CP-02-004 @Error @CategoriaInactiva
  Escenario: Registro fallido por categoría inactiva
    Dado que existe la categoría "Barbería" pero está inactiva
    Cuando el proveedor intenta registrarse con esa categoría
    Entonces el sistema muestra el error "La categoría seleccionada no existe o no está activa"
    Y no crea la cuenta del proveedor

  @CP-02-005 @Error @EmailDuplicado
  Escenario: Registro fallido por correo duplicado entre roles
    Dado que ya existe un cliente con el correo "salon@bellavida.com"
    Cuando el proveedor intenta registrarse con ese mismo correo
    Entonces el sistema muestra el error "El correo electrónico ya está en uso"
    Y no crea una nueva cuenta
