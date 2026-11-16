# Trazabilidad de la entrega

Fuente academica: *Criterios de evaluación del proyecto final SDD*, 8 páginas,
Ingenieria Web. El documento no obliga a inventar más funcionalidades de marketplace;
pide coherencia entre propuesta, plan, implementacion y evidencia.

## Rúbrica a repositorio

| Criterio / página | Archivos que lo atienden | Evidencia y límite |
|---|---|---|
| Artefactos SDD, p. 3 | constitution.md, spec.md, plan.md, tasks.md | Preparados; deben versionarse en Git |
| Aplicación y calidad, p. 4 | src/main, migraciones, API.md | Código refactorizado; compilación completa pendiente |
| Trabajo colaborativo, p. 5 | CONTRIBUTING.md, plantilla PR, issues | Flujo individual preparado; sin inventar equipo ni historial |
| Pruebas, p. 6 | src/test, tools, workflow y VERIFICACION.md | Resultados disponibles separados de pruebas no ejecutadas |
| README/demo, p. 6 | README.md, DESPLIEGUE.md, EXHIBICION.md | Guia, configuración, decisiones y flujo; no demuestra una presentacion hecha |
| Entregables, p. 8 | Repositorio organizado completo | Falta subirlo, ejecutar CI y presentar con evidencia real |

Los enlaces de entrada estan en el README. El uso efectivo del CLI de Spec Kit y
la aceptacion de una entrega individual siguen pendientes; archivos preparados no
equivalen a satisfacer automáticamente todas las evidencias de la rúbrica.

## Especificación a código y pruebas

Los métodos citados son pruebas incluidas, no resultados de ejecución de JUnit.

| Criterio | Implementacion | Prueba / evidencia preparada |
|---|---|---|
| AC1.1-1.3 | AuthService, UserRequests, PasswordPolicy | FlowIntegrationTest.registrationAndPublicationValidationProtectData, logoutRevokesSessionAndAdminEndpointsRejectOrdinaryUsers, sqlRadiusFilterAndPasswordRecoveryWork |
| AC1.4 | AuthRateLimiter, FixedWindowRateLimiter | BackendRulesTest.limiterExpiresAndNeverExceedsCapacity, limiterSerializesConcurrentRequests |
| AC1.5 | UserRepository, ProfileService | FlowIntegrationTest.completeFlowHasPermissionsPrivacyHistoryAndMutualReviews |
| AC2.1-2.2 | JobService, JobRequests | FlowIntegrationTest.registrationAndPublicationValidationProtectData |
| AC2.3 | JobRules, JobService | pricesAreFrozenPerPersonAndAutomaticIncreasesAreCapped y JobRulesTest |
| AC2.4-2.5 | JobService.accept, bloqueo SQL | twoWorkersCannotTakeLastSlot y completeFlowHasPermissionsPrivacyHistoryAndMutualReviews |
| AC2.6 | JobRepository.hasParticipation / isAssigned | withdrawnWorkerKeepsHistoryButLosesExactAddress |
| AC2.7 | JobRepository, PageWindow | sqlRadiusFilterAndPasswordRecoveryWork, paginationCannotOverflowIntegerRange |
| AC3.1-3.3 | ChatService, JobService, ReviewService | completeFlowHasPermissionsPrivacyHistoryAndMutualReviews |
| AC3.4 | AdminService, ReportService, NotificationService | adminMayModerateButOrdinaryUsersCannot |
| AC3.5 | events y servicios transaccionales | eventos persistidos en el flujo; no se prueba una cola externa |
| AC4.1-4.2 | ImageSanitizer, MediaService | ImageSanitizerTest y mediaIsRealPrivateAndRejectsArbitraryFiles |
| AC4.3 | ProfileService y bloqueo de media | avatarCannotBeReusedAsJobAttachment |
| AC4.4 | SupabaseObjectStorage, ApiErrors | rutas controladas en código; falta probar el proveedor real |
| AC4.5 | MediaService, sincronizacion de transacción | MediaCompensationTest (dobles Mockito; no corte real de red) |
| AC5.1-5.3 | Dockerfile, perfiles, CloudConfigurationGuard | CloudConfigurationGuardTest; despliegue real pendiente |
| AC5.4-5.6 | docs/specs, CI, V2 | check_repository.py, hash V1 e inventario de rutas |

El registro de 66 comprobaciones sin dependencias válida reglas puras, no reemplaza
los métodos Spring/JUnit anteriores. El resultado de Actions se adjuntara cuando
exista una ejecución real.
