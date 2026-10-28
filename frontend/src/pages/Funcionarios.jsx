import CrudPage from '../components/CrudPage';
import { CARGOS } from '../services/codigos';
import { date, money } from '../utils/format';
import { useAuth } from '../contexts/AuthContext';

const STATUS = [{ value: 'ATIVO', label: 'Ativo' }, { value: 'FERIAS', label: 'Férias' }, { value: 'INATIVO', label: 'Inativo' }];

export default function Funcionarios() {
  const { user } = useAuth();
  // O servidor só mostra e só altera salário para DONO e GERENTE.
  const verSalario = ['DONO', 'GERENTE'].includes(user?.cargo);
  const fields = [
    { name: 'nome', label: 'Nome', required: true, full: true },
    { name: 'cargoId', label: 'Cargo', required: true, options: CARGOS.map((c) => ({ value: c, label: c })), default: 'VENDEDOR' },
    { name: 'status', label: 'Status', options: STATUS, default: 'ATIVO' },
    { name: 'telefone', label: 'Telefone' },
    { name: 'email', label: 'E-mail (o mesmo do usuário, para entregadores)', type: 'email' },
    { name: 'endereco', label: 'Endereço', full: true },
    ...(verSalario ? [{ name: 'salario', label: 'Salário (R$)', type: 'number' }] : []),
    { name: 'dataContratacao', label: 'Contratação', type: 'date' },
    { name: 'observacoes', label: 'Observações', full: true }
  ];
  const columns = [
    { key: 'nome', label: 'Nome' }, { key: 'cargo', label: 'Cargo' }, { key: 'status', label: 'Status' }, { key: 'telefone', label: 'Telefone' },
    { key: 'dataContratacao', label: 'Contratação', render: (f) => date(f.dataContratacao) },
    ...(verSalario ? [{ key: 'salario', label: 'Salário', render: (f) => (f.salario == null ? '—' : money(f.salario)) }] : [])
  ];
  return <CrudPage title="Funcionários" subtitle="Equipe da loja e da produção." endpoint="/funcionarios" singular="funcionário" fields={fields} columns={columns}
    permissions={{ create: 'funcionarios_criar', edit: 'funcionarios_editar', remove: 'funcionarios_excluir' }}
    toForm={(f) => ({ ...f, cargoId: f.cargo })}
    toPayload={(d) => ({ ...d, salario: d.salario === '' || d.salario == null ? null : Number(d.salario), dataContratacao: d.dataContratacao || null, email: d.email || null })} />;
}
