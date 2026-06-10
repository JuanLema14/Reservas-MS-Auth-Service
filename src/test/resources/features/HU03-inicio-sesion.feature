#language: es
@InicioSesion @HU03
Característica: Inicio de sesión
  Como usuario registrado (cliente o proveedor)
  Quiero iniciar sesión en la plataforma
  Para acceder a mis funcionalidades según mi rol

  Antecedentes:
    Dado que el sistema está operativo

  @CP-03-001 @HappyPath
  Escenario: Login exitoso como cliente
    Dado que existe un cliente registrado con correo "carlos@email.com" y contraseña "Segura#123"
    Cuando el usuario ingresa su correo "carlos@email.com"
    Y ingresa su contraseña "Segura#123"
    Y hace clic en "Iniciar sesión"
    Entonces el sistema autentica al usuario exitosamente
    Y genera un token JWT de acceso
    Y genera un refresh token
    Y el token JWT contiene el rol "CLIENTE"
    Y el token JWT contiene el email "carlos@email.com"
    Y lo redirige al panel de cliente

  @CP-03-002 @HappyPath
  Escenario: Login exitoso como proveedor
    Dado que existe un proveedor registrado con correo "salon@bellavida.com" y contraseña "Segura#123"
    Cuando el proveedor ingresa sus credenciales correctas
    Y hace clic en "Iniciar sesión"
    Entonces el sistema autentica al proveedor exitosamente
    Y genera un token JWT de acceso
    Y el token JWT contiene el rol "PROVEEDOR"
    Y lo redirige al panel de administración de servicios

  @CP-03-003 @Error @PasswordIncorrecta
  Escenario: Login fallido por contraseña incorrecta
    Dado que existe un cliente registrado con correo "carlos@email.com"
    Cuando el usuario ingresa su correo "carlos@email.com"
    Y ingresa una contraseña incorrecta "WrongPass123"
    Y hace clic en "Iniciar sesión"
    Entonces el sistema muestra el error "Credenciales inválidas"
    Y no genera ningún token de acceso
    Y el código de respuesta HTTP es 401

  @CP-03-004 @Error @UsuarioNoExiste
  Escenario: Login fallido por usuario no registrado
    Dado que no existe un usuario con correo "noexiste@email.com"
    Cuando el usuario intenta iniciar sesión con ese correo
    Entonces el sistema muestra el error "Credenciales inválidas"
    Y no concede acceso

  @CP-03-005 @HappyPath @RefreshToken
  Escenario: Refrescar token exitoso
    Dado que el usuario tiene un refresh token válido
    Cuando solicita un nuevo token de acceso con el refresh token
    Entonces el sistema genera un nuevo token JWT
    Y el refresh token anterior se invalida
    Y se genera un nuevo refresh token

  @CP-03-006 @Error @RefreshTokenInvalido
  Escenario: Refrescar token con token inválido
    Dado que el usuario proporciona un refresh token inválido
    Cuando solicita un nuevo token de acceso
    Entonces el sistema muestra el error "Refresh token inválido"
    Y no genera ningún token nuevo

  @CP-03-007 @HappyPath @Logout
  Escenario: Cerrar sesión exitosamente
    Dado que el usuario tiene una sesión activa
    Cuando solicita cerrar sesión
    Entonces el sistema invalida el refresh token
    Y el token de acceso ya no es válido
