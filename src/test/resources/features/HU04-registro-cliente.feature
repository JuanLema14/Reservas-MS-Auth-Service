#language: es
@RegistroCliente @HU04
Característica: Registro de usuario cliente
  Como persona interesada en reservar servicios
  Quiero registrarme en la plataforma como cliente
  Para poder acceder y gestionar mis reservas

  Antecedentes:
    Dado que el sistema está operativo

  @CP-01-001 @HappyPath
  Escenario: Registro exitoso de un nuevo cliente
    Dado que el usuario no tiene cuenta en la plataforma
    Cuando ingresa su nombre completo "Carlos Pérez"
    Y ingresa su correo electrónico "carlos@email.com"
    Y ingresa una contraseña válida "Segura#123"
    Y ingresa su teléfono "3001234567"
    Y selecciona el rol "Cliente"
    Y hace clic en "Registrarse"
    Entonces el sistema crea la cuenta exitosamente
    Y el sistema retorna un ID de usuario único
    Y el tipo de usuario es "CLIENTE"
    Y muestra el mensaje "¡Registro exitoso! Bienvenido, Carlos Pérez"
    Y la contraseña se almacena de forma hasheada
    Y redirige al usuario al panel principal

  @CP-01-002 @Error @EmailDuplicado
  Escenario: Registro fallido por correo ya existente
    Dado que ya existe una cuenta con el correo "carlos@email.com"
    Cuando el usuario intenta registrarse con los mismos datos
    Entonces el sistema muestra el error "El correo electrónico ya está en uso"
    Y no crea una nueva cuenta
    Y el código de respuesta HTTP es 409

  @CP-01-003 @Error @DatosIncompletos
  Escenario: Registro fallido por datos incompletos
    Dado que el usuario está en el formulario de registro
    Cuando deja el campo "nombre" vacío
    Y completa los demás campos correctamente
    Y hace clic en "Registrarse"
    Entonces el sistema muestra el error "El nombre es obligatorio"
    Y no procesa el registro

  @CP-01-004 @Error @PasswordDebil
  Escenario: Registro fallido por contraseña débil
    Dado que el usuario está en el formulario de registro
    Cuando ingresa una contraseña débil "12345"
    Y completa los demás campos correctamente
    Y hace clic en "Registrarse"
    Entonces el sistema muestra el error "La contraseña debe contener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial"
    Y no procesa el registro